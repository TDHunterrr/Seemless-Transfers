package net.seemless.transfers.network;

/**
 * Universal placeholder resolver for Seemless Transfers.
 * Parses input formats across PlaceholderAPI, Text Placeholder API, and MiniPlaceholders
 * into host/port queries and returns formatted real-time data.
 *
 * Authored by TD_Hunterrr. All Rights Reserved.
 */
public final class PlaceholderResolver {

    private PlaceholderResolver() {
    }

    /**
     * Resolves a placeholder query string into real-time server information.
     *
     * @param input Raw query input (e.g. "127.0.0.1_25565", "127.0.0.1:25565", "sl_127.0.0.1_25565")
     * @param defaultType Default query type ("online", "max", "ping", "status")
     * @return Formatted real-time result string
     */
    public static String resolve(String input, String defaultType) {
        if (input == null || input.trim().isEmpty()) {
            return "0";
        }

        String params = input.trim();

        // Strip redundant "sl_" prefix if present
        if (params.startsWith("sl_")) {
            params = params.substring(3);
        }

        String queryType = defaultType != null ? defaultType : "online";
        if (params.startsWith("online_") || params.startsWith("online:") || params.startsWith("online ")) {
            queryType = "online";
            params = params.substring(7);
        } else if (params.startsWith("max_") || params.startsWith("max:") || params.startsWith("max ")) {
            queryType = "max";
            params = params.substring(4);
        } else if (params.startsWith("ping_") || params.startsWith("ping:") || params.startsWith("ping ")
                || params.startsWith("latency_") || params.startsWith("latency:") || params.startsWith("latency ")) {
            queryType = "ping";
            int idx = -1;
            for (char c : new char[]{'_', ':', ' '}) {
                int i = params.indexOf(c);
                if (i != -1 && (idx == -1 || i < idx)) {
                    idx = i;
                }
            }
            if (idx != -1) {
                params = params.substring(idx + 1);
            }
        } else if (params.startsWith("status_") || params.startsWith("status:") || params.startsWith("status ")) {
            queryType = "status";
            params = params.substring(7);
        }

        String host = params.trim();
        int port = ServerPinger.DEFAULT_PORT;

        int sep = -1;
        if (host.contains(":")) {
            sep = host.lastIndexOf(':');
        } else if (host.contains("_")) {
            sep = host.lastIndexOf('_');
        } else if (host.contains(" ")) {
            sep = host.lastIndexOf(' ');
        }

        if (sep != -1) {
            String pStr = host.substring(sep + 1).trim();
            try {
                int p = Integer.parseInt(pStr);
                if (p >= 1 && p <= 65535) {
                    port = p;
                    host = host.substring(0, sep).trim();
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (host.isEmpty()) {
            return "0";
        }

        ServerPinger.ServerStatus status = ServerPinger.getStatus(host, port);
        return switch (queryType) {
            case "online" -> String.valueOf(status.onlinePlayers());
            case "max" -> String.valueOf(status.maxPlayers());
            case "ping" -> String.valueOf(status.latencyMs() >= 0 ? status.latencyMs() : -1);
            case "status" -> status.isOnline() ? "Online" : "Offline";
            default -> String.valueOf(status.onlinePlayers());
        };
    }
}
