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
        return plugin.getPluginMeta().getVersion();
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
        return net.seemless.transfers.network.PlaceholderResolver.resolve(params, "online");
    }
}
