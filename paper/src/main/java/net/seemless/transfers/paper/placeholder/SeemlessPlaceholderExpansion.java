package net.seemless.transfers.paper.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.seemless.transfers.network.ServerPinger;

/**
 * PlaceholderAPI expansion for Seemless Transfers.
 * Provides real-time server player counts via %sl_<ip>_<port>%.
 *
 * Authored by TD_Hunterrr. All Rights Reserved.
 */
public class SeemlessPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;

    public SeemlessPlaceholderExpansion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "sl";
    }

    @Override
    public String getAuthor() {
        return "TD_Hunterrr";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.trim().isEmpty()) {
            return null;
        }

        String input = params.trim();

        // Strip optional redundant "sl_" prefix (e.g. if invoked as %sl_sl_<ip>_<port>%)
        if (input.startsWith("sl_")) {
            input = input.substring(3);
        }

        String queryType = "online";
        if (input.startsWith("online_")) {
            queryType = "online";
            input = input.substring(7);
        } else if (input.startsWith("max_")) {
            queryType = "max";
            input = input.substring(4);
        } else if (input.startsWith("ping_") || input.startsWith("latency_")) {
            queryType = "ping";
            input = input.substring(input.indexOf('_') + 1);
        } else if (input.startsWith("status_")) {
            queryType = "status";
            input = input.substring(7);
        }

        String host = input;
        int port = ServerPinger.DEFAULT_PORT;

        // Parse host and port from format "<ip>_<port>" or "<ip>:<port>"
        int separatorIndex = -1;
        if (input.contains(":")) {
            separatorIndex = input.lastIndexOf(':');
        } else if (input.contains("_")) {
            separatorIndex = input.lastIndexOf('_');
        }

        if (separatorIndex != -1) {
            String portString = input.substring(separatorIndex + 1);
            try {
                int parsedPort = Integer.parseInt(portString);
                if (parsedPort >= 1 && parsedPort <= 65535) {
                    port = parsedPort;
                    host = input.substring(0, separatorIndex);
                }
            } catch (NumberFormatException ignored) {
                // Not a valid port number; entire input is treated as the hostname
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
