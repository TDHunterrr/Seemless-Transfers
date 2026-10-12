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

    public static final class PreloadTarget {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final long timestamp;

        public PreloadTarget(double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }

        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public float yaw() { return yaw; }
        public float pitch() { return pitch; }
        public long timestamp() { return timestamp; }
    }
    private static final Map<UUID, PreloadTarget> PRELOAD_TARGETS = new ConcurrentHashMap<>();

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
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("PRELOAD ")) {
                String payload = line.substring(8);
                // format: UUID:PLAYER_NAME:X:Y:Z:YAW:PITCH or UUID:PLAYER_NAME:X:Y:Z
                String[] parts = payload.split(":");
                if (parts.length >= 7) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        float yaw = Float.parseFloat(parts[5]);
                        float pitch = Float.parseFloat(parts[6]);
                        PRELOAD_TARGETS.put(uuid, new PreloadTarget(x, y, z, yaw, pitch, System.currentTimeMillis()));
                    } catch (Exception ignored) {
                    }
                } else if (parts.length >= 5) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        double x = Double.parseDouble(parts[2]);
                        double y = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        PRELOAD_TARGETS.put(uuid, new PreloadTarget(x, y, z, 0.0f, 0.0f, System.currentTimeMillis()));
                    } catch (Exception ignored) {
                    }
                }
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
            for (int port : candidatePorts) {
                if (port <= 0 || port > 65535) continue;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(targetHost, port), 250);
                    socket.setSoTimeout(1000);
                    try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                        writer.println("PRELOAD " + uuid + ":" + name + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch);
                        return; // Successfully sent to target server
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static void putTarget(UUID uuid, PreloadTarget target) {
        PRELOAD_TARGETS.put(uuid, target);
    }

    public static PreloadTarget getAndConsumeTarget(UUID uuid) {
        PreloadTarget target = PRELOAD_TARGETS.remove(uuid);
        if (target == null) return null;
        if (System.currentTimeMillis() - target.timestamp() > 60_000) {
            return null;
        }
        return target;
    }
}
