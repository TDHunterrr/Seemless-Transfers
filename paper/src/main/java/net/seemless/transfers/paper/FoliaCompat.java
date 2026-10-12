package net.seemless.transfers.paper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Universal compatibility utility providing unified scheduling and entity operations
 * across Folia (PaperMC regional multithreading) and standard Paper / Purpur servers.
 *
 * Authored by TD_Hunterrr. All Rights Reserved.
 */
public final class FoliaCompat {

    /**
     * Whether the current runtime is a Folia server.
     */
    public static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = folia;
    }

    private FoliaCompat() {
    }

    /**
     * Executes a task on the player's regional tick thread (EntityScheduler on Folia, BukkitScheduler on Paper).
     */
    public static void runOnPlayer(Plugin plugin, Player player, Runnable runnable) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (IS_FOLIA) {
            player.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Executes a delayed task on the player's regional tick thread.
     */
    public static void runDelayedOnPlayer(Plugin plugin, Player player, Runnable runnable, long delayTicks) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (IS_FOLIA) {
            player.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    /**
     * Executes a global task (GlobalRegionScheduler on Folia, BukkitScheduler on Paper).
     */
    public static void runGlobal(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Asynchronously preloads a chunk in the specified world.
     */
    public static void preloadChunkAsync(World world, int chunkX, int chunkZ) {
        if (world != null) {
            world.getChunkAtAsync(chunkX, chunkZ);
        }
    }
}
