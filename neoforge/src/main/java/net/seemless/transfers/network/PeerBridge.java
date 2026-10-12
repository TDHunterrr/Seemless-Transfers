package net.seemless.transfers.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PeerBridge {
    public static final int DEFAULT_PEER_PORT = 25585;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static ServerSocket serverSocket;
    private static volatile boolean running = false;

    public static class PreloadTarget extends net.seemless.transfers.network.PreloadTarget {
        public PreloadTarget(double x, double y, double z, float yaw, float pitch, long timestamp) {
            super(x, y, z, yaw, pitch, timestamp);
        }
    }
    private static final Map<UUID, PreloadTarget> PRELOAD_TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, PreloadTarget> PRELOAD_TARGETS_BY_NAME = new ConcurrentHashMap<>();

    public static synchronized void startServer(int preferredPort, Consumer<String> preloadHandler) {
        if (running) return;
        running = true;
        EXECUTOR.submit(() -> {
            int[] portsToTry = new int[]{
                    preferredPort + 10,
                    preferredPort + 1000,
                    DEFAULT_PEER_PORT,
                    DEFAULT_PEER_PORT + 10,
                    DEFAULT_PEER_PORT + 1,
                    DEFAULT_PEER_PORT + 2,
                    preferredPort
            };
            for (int p : portsToTry) {
                if (p <= 0 || p > 65535) continue;
                try {
                    serverSocket = new ServerSocket(p);
                    System.out.println("[SeemlessTransfers] PeerBridge server listening on port " + p);
                    break;
                } catch (Exception ignored) {
                }
            }
            if (serverSocket == null || serverSocket.isClosed()) {
                System.err.println("[SeemlessTransfers] Warning: Could not bind PeerBridge on any port.");
                return;
            }
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    EXECUTOR.submit(() -> handleConnection(socket, preloadHandler));
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static synchronized void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {
        }
    }

    private static void handleConnection(Socket socket, Consumer<String> preloadHandler) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            socket.setSoTimeout(2000);
            String line = reader.readLine();
            if (line != null && line.startsWith("PRELOAD ")) {
                String payload = line.substring(8);
                // format: UUID:PLAYER_NAME:X:Y:Z:YAW:PITCH or UUID:PLAYER_NAME:X:Y:Z
                String[] parts = payload.split(":");
                if (parts.length >= 7) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        String playerName = parts[1];
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        float yaw = Float.parseFloat(parts[5]);
                        float pitch = Float.parseFloat(parts[6]);
                        PreloadTarget target = new PreloadTarget(x, y, z, yaw, pitch, System.currentTimeMillis());
                        PRELOAD_TARGETS.put(uuid, target);
                        if (playerName != null && !playerName.isEmpty()) {
                            PRELOAD_TARGETS_BY_NAME.put(playerName.toLowerCase(java.util.Locale.ROOT), target);
                        }
                    } catch (Exception ignored) {
                    }
                } else if (parts.length >= 5) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        String playerName = parts[1];
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        PreloadTarget target = new PreloadTarget(x, y, z, 0.0f, 0.0f, System.currentTimeMillis());
                        PRELOAD_TARGETS.put(uuid, target);
                        if (playerName != null && !playerName.isEmpty()) {
                            PRELOAD_TARGETS_BY_NAME.put(playerName.toLowerCase(java.util.Locale.ROOT), target);
                        }
                    } catch (Exception ignored) {
                    }
                }
                writer.println("ACK");
                if (preloadHandler != null) {
                    preloadHandler.accept(payload);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void sendPreload(String targetHost, int targetGamePort, UUID uuid, String name,
                                  double x, double y, double z, float yaw, float pitch) {
        EXECUTOR.submit(() -> {
            int[] candidatePorts = new int[]{
                    targetGamePort + 10,
                    targetGamePort + 1000,
                    DEFAULT_PEER_PORT,
                    DEFAULT_PEER_PORT + 10,
                    DEFAULT_PEER_PORT + 1,
                    DEFAULT_PEER_PORT + 2,
                    targetGamePort
            };

            java.util.List<java.net.InetAddress> targetAddresses = new java.util.ArrayList<>();
            try {
                java.net.InetAddress[] addrs = java.net.InetAddress.getAllByName(targetHost);
                for (java.net.InetAddress a : addrs) {
                    if (!targetAddresses.contains(a)) targetAddresses.add(a);
                }
            } catch (Exception ignored) {
            }

            if ("localhost".equalsIgnoreCase(targetHost) || "127.0.0.1".equals(targetHost)) {
                try {
                    java.net.InetAddress ipv4 = java.net.InetAddress.getByName("127.0.0.1");
                    if (!targetAddresses.contains(ipv4)) targetAddresses.add(0, ipv4);
                } catch (Exception ignored) {
                }
            }

            for (int port : candidatePorts) {
                if (port <= 0 || port > 65535) continue;
                for (java.net.InetAddress addr : targetAddresses) {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(addr, port), 400);
                        socket.setSoTimeout(1500);
                        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                            writer.println("PRELOAD " + uuid + ":" + name + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch);
                            String ack = reader.readLine();
                            System.out.println("[SeemlessTransfers] PeerBridge preload confirmed for " + name
                                    + " on " + addr.getHostAddress() + ":" + port + " (" + ack + ")");
                            return; // Confirmed delivery to target server
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            System.err.println("[SeemlessTransfers] PeerBridge notice: Preload socket could not reach " + targetHost + ":" + targetGamePort);
        });
    }

    public static void putTarget(UUID uuid, PreloadTarget target) {
        putTarget(uuid, null, target);
    }

    public static void putTarget(UUID uuid, String name, PreloadTarget target) {
        if (uuid != null) PRELOAD_TARGETS.put(uuid, target);
        if (name != null && !name.isEmpty()) {
            PRELOAD_TARGETS_BY_NAME.put(name.toLowerCase(java.util.Locale.ROOT), target);
        }
    }

    public static PreloadTarget getAndConsumeTarget(UUID uuid) {
        return getAndConsumeTarget(uuid, null);
    }

    public static PreloadTarget getAndConsumeTarget(UUID uuid, String name) {
        PreloadTarget target = null;
        if (uuid != null) {
            target = PRELOAD_TARGETS.remove(uuid);
        }
        if (target == null && name != null && !name.isEmpty()) {
            target = PRELOAD_TARGETS_BY_NAME.remove(name.toLowerCase(java.util.Locale.ROOT));
        }
        if (target != null) {
            if (uuid != null) PRELOAD_TARGETS.remove(uuid);
            if (name != null && !name.isEmpty()) PRELOAD_TARGETS_BY_NAME.remove(name.toLowerCase(java.util.Locale.ROOT));
            if (System.currentTimeMillis() - target.timestamp() > 60_000) {
                return null;
            }
        }
        return target;
    }
}
