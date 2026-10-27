package net.seemless.transfers.network;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ultra-fast, asynchronous Minecraft Server List Ping (SLP) client and cache.
 * Queries any remote Minecraft server in real-time to fetch online player counts,
 * max player limits, latency, and operational status without stalling the server tick loop.
 *
 * Authored by TD_Hunterrr. All Rights Reserved.
 */
public final class ServerPinger {

    public static final int DEFAULT_PORT = 25565;
    public static final int CONNECT_TIMEOUT_MS = 2000;
    public static final long CACHE_TTL_MS = 2000L;          // 2 seconds for fresh real-time player counts
    public static final long OFFLINE_CACHE_TTL_MS = 4000L;  // 4 seconds when offline to prevent socket thrashing

    private static final Pattern ONLINE_PATTERN = Pattern.compile("\"players\"\\s*:\\s*\\{[^}]*?\"online\"\\s*:\\s*(\\d+)");
    private static final Pattern MAX_PATTERN = Pattern.compile("\"players\"\\s*:\\s*\\{[^}]*?\"max\"\\s*:\\s*(\\d+)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\\{[^}]*?\"name\"\\s*:\\s*\"([^\"]+)\"");

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Seemless-Pinger-" + THREAD_COUNTER.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<String, ServerStatus> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<ServerStatus>> IN_FLIGHT = new ConcurrentHashMap<>();

    private ServerPinger() {
    }

    /**
     * Snapshot record containing server ping details.
     */
    public record ServerStatus(
            boolean isOnline,
            int onlinePlayers,
            int maxPlayers,
            long latencyMs,
            String versionName,
            long timestamp
    ) {
        public static ServerStatus offline(long timestamp) {
            return new ServerStatus(false, 0, 0, -1L, "Offline", timestamp);
        }
    }

    /**
     * Retrieves the cached online player count for the target server, triggering an asynchronous
     * background ping if the cached value is stale or missing.
     *
     * @param host Target server IP or hostname
     * @param port Target server game port
     * @return Real-time online player count (or 0 if offline/pending)
     */
    public static int getOnlinePlayers(String host, int port) {
        return getStatus(host, port).onlinePlayers();
    }

    /**
     * Retrieves the server status snapshot, initiating an asynchronous background ping if needed.
     *
     * @param host Target server IP or hostname
     * @param port Target server game port
     * @return The latest ServerStatus snapshot
     */
    public static ServerStatus getStatus(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            return ServerStatus.offline(System.currentTimeMillis());
        }

        String key = (host.trim() + ":" + (port > 0 ? port : DEFAULT_PORT)).toLowerCase();
        long now = System.currentTimeMillis();
        ServerStatus cached = CACHE.get(key);

        if (cached == null) {
            triggerAsyncPing(host.trim(), port > 0 ? port : DEFAULT_PORT, key);
            return ServerStatus.offline(now);
        }

        long ttl = cached.isOnline() ? CACHE_TTL_MS : OFFLINE_CACHE_TTL_MS;
        if (now - cached.timestamp() > ttl) {
            triggerAsyncPing(host.trim(), port > 0 ? port : DEFAULT_PORT, key);
        }

        return cached;
    }

    /**
     * Asynchronously pings a remote Minecraft server and returns a CompletableFuture with the status.
     */
    public static CompletableFuture<ServerStatus> pingAsync(String host, int port) {
        String key = (host.trim() + ":" + (port > 0 ? port : DEFAULT_PORT)).toLowerCase();
        return triggerAsyncPing(host.trim(), port > 0 ? port : DEFAULT_PORT, key);
    }

    private static CompletableFuture<ServerStatus> triggerAsyncPing(String host, int port, String cacheKey) {
        return IN_FLIGHT.computeIfAbsent(cacheKey, k -> CompletableFuture.supplyAsync(() -> pingSync(host, port), EXECUTOR)
                .whenComplete((result, ex) -> {
                    IN_FLIGHT.remove(cacheKey);
                    if (result != null) {
                        CACHE.put(cacheKey, result);
                    } else {
                        CACHE.put(cacheKey, ServerStatus.offline(System.currentTimeMillis()));
                    }
                }));
    }

    /**
     * Synchronously connects and pings the target server using standard Minecraft SLP protocol.
     */
    public static ServerStatus pingSync(String host, int port) {
        long startTime = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            InputStream in = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(in);

            // 1. Handshake Packet (0x00)
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(bOut);
            writeVarInt(handshake, 0x00);       // Packet ID 0
            writeVarInt(handshake, 766);        // Protocol version (1.20.6)
            writeString(handshake, host);       // Server address
            handshake.writeShort(port);         // Server port
            writeVarInt(handshake, 1);          // Next state: 1 (status)

            writeVarInt(dataOut, bOut.size());
            dataOut.write(bOut.toByteArray());

            // 2. Status Request Packet (0x00)
            dataOut.writeByte(0x01); // Packet length 1
            dataOut.writeByte(0x00); // Packet ID 0
            dataOut.flush();

            // 3. Read Response Packet
            int packetLength = readVarInt(dataIn);
            if (packetLength <= 0) {
                return ServerStatus.offline(System.currentTimeMillis());
            }

            int packetId = readVarInt(dataIn);
            if (packetId != 0x00) {
                return ServerStatus.offline(System.currentTimeMillis());
            }

            String jsonResponse = readString(dataIn);
            long latency = Math.max(1, System.currentTimeMillis() - startTime);

            int online = parseRegexGroup(ONLINE_PATTERN, jsonResponse, 0);
            int max = parseRegexGroup(MAX_PATTERN, jsonResponse, 0);
            String version = parseRegexString(VERSION_PATTERN, jsonResponse, "Unknown");

            return new ServerStatus(true, online, max, latency, version, System.currentTimeMillis());
        } catch (Throwable ignored) {
            return ServerStatus.offline(System.currentTimeMillis());
        }
    }

    private static int parseRegexGroup(Pattern pattern, String text, int defaultValue) {
        try {
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private static String parseRegexString(Pattern pattern, String text, String defaultValue) {
        try {
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 65535) {
            throw new IOException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Shuts down background pinging daemon threads.
     */
    public static void shutdown() {
        try {
            EXECUTOR.shutdownNow();
            CACHE.clear();
            IN_FLIGHT.clear();
        } catch (Throwable ignored) {
        }
    }
}
