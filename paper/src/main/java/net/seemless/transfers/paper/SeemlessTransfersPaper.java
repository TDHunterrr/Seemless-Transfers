package net.seemless.transfers.paper;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import net.seemless.transfers.network.PeerBridge;
import net.seemless.transfers.network.TransferToken;

public class SeemlessTransfersPaper extends JavaPlugin implements Listener {

    private final NamespacedKey cookieKey = new NamespacedKey(TransferToken.COOKIE_NAMESPACE, TransferToken.COOKIE_PATH);

    @Override
    public void onLoad() {
        try {
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                final Commands commands = event.registrar();
                commands.register(
                        Commands.literal("sl")
                                .requires(source -> source.getSender().isOp() || source.getSender().hasPermission("seemless.admin"))
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .then(Commands.argument("ip", StringArgumentType.string())
                                                .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg(TransferToken.MIN_XZ, TransferToken.MAX_XZ))
                                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg(TransferToken.MIN_Y, TransferToken.MAX_Y))
                                                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg(TransferToken.MIN_XZ, TransferToken.MAX_XZ))
                                                                                .executes(ctx -> executeTransfer(ctx, 0.0f, 0.0f))
                                                                                .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(-180.0, 180.0))
                                                                                        .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(-90.0, 90.0))
                                                                                                .executes(ctx -> {
                                                                                                    float yaw = (float) DoubleArgumentType.getDouble(ctx, "yaw");
                                                                                                    float pitch = (float) DoubleArgumentType.getDouble(ctx, "pitch");
                                                                                                    return executeTransfer(ctx, yaw, pitch);
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ).build(),
                        "Seamless server transfer command"
                );
            });
        } catch (Throwable t) {
            getLogger().warning("Could not register Paper Brigadier lifecycle event: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("Seemless Transfers (Paper) enabled!");

        // Automatically disable connection throttle so fast transfers are never blocked or warned
        disableConnectionThrottle();

        getServer().getPluginManager().registerEvents(this, this);

        // Start peer bridge to listen for pre-load notifications from other servers
        int gamePort = Bukkit.getPort() > 0 ? Bukkit.getPort() : 25565;
        PeerBridge.startServer(
                gamePort,
                payload -> {
                    disableConnectionThrottle();
                    try {
                        String[] parts = payload.split(":");
                        if (parts.length >= 7) {
                            UUID uuid = UUID.fromString(parts[0]);
                            double x = Double.parseDouble(parts[2]);
                            double y = Double.parseDouble(parts[3]);
                            double z = Double.parseDouble(parts[4]);
                            float yaw = Float.parseFloat(parts[5]);
                            float pitch = Float.parseFloat(parts[6]);

                            Player onlinePlayer = Bukkit.getPlayer(uuid);
                            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                FoliaCompat.runOnPlayer(this, onlinePlayer, () -> {
                                    teleportToTarget(onlinePlayer, x, y, z, yaw, pitch);
                                    getLogger().info("Teleported already-online player " + onlinePlayer.getName() + " to preload target!");
                                });
                            } else if (!Bukkit.getWorlds().isEmpty()) {
                                org.bukkit.World world = Bukkit.getWorlds().get(0);
                                int chunkX = (int) x >> 4;
                                int chunkZ = (int) z >> 4;
                                FoliaCompat.preloadChunkAsync(world, chunkX, chunkZ);
                                getLogger().info("Pre-loaded chunk (" + chunkX + ", " + chunkZ + ") for incoming player!");
                            }
                        }
                    } catch (Exception e) {
                        getLogger().warning("Failed to parse preload payload: " + e.getMessage());
                    }
                }
        );

        // Register PlaceholderAPI expansion if installed
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new net.seemless.transfers.paper.placeholder.SeemlessPlaceholderExpansion(this).register();
                getLogger().info("Hooked into PlaceholderAPI! Registered %sl_<ip>_<port>% expansion for real-time player counts.");
            } catch (Throwable t) {
                getLogger().warning("Could not register PlaceholderAPI expansion: " + t.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        net.seemless.transfers.network.ServerPinger.shutdown();
        PeerBridge.stopServer();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Check instant PeerBridge memory cache (by UUID and Name)
        PeerBridge.PreloadTarget peerTarget = PeerBridge.getAndConsumeTarget(player.getUniqueId(), player.getName());
        if (peerTarget != null) {
            teleportToTarget(player, peerTarget.x(), peerTarget.y(), peerTarget.z(), peerTarget.yaw(), peerTarget.pitch());
            return;
        }

        // 2. Fetch native cryptographic transfer cookie carried directly by the client across the internet!
        try {
            player.retrieveCookie(cookieKey).thenAccept(cookieBytes -> {
                if (cookieBytes != null && cookieBytes.length > 0) {
                    TransferToken.LocationTarget cookieTarget = TransferToken.parseAndVerifyToken(cookieBytes, player.getUniqueId());
                    if (cookieTarget != null) {
                        teleportToTarget(player, cookieTarget.x(), cookieTarget.y(), cookieTarget.z(), cookieTarget.yaw(), cookieTarget.pitch());
                    }
                }
            });
        } catch (Exception e) {
            getLogger().warning("Failed to retrieve transfer cookie: " + e.getMessage());
        }
    }

    private void teleportToTarget(Player player, double x, double y, double z, float yaw, float pitch) {
        FoliaCompat.runOnPlayer(this, player, () -> {
            if (!player.isOnline()) return;
            org.bukkit.World world = player.getWorld();
            Location loc = new Location(world, x, y, z, yaw, pitch);

            // teleportAsync handles asynchronous chunk generation/loading and cross-region ticking safely on both Folia and Paper
            player.teleportAsync(loc).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    getLogger().info("Teleported incoming player " + player.getName() + " to "
                            + String.format("%.1f, %.1f, %.1f (yaw: %.1f, pitch: %.1f)", x, y, z, yaw, pitch));
                }
            });
        });
    }

    private int executeTransfer(CommandContext<CommandSourceStack> ctx, float yaw, float pitch) {
        CommandSender sender = ctx.getSource().getSender();
        try {
            PlayerSelectorArgumentResolver selector = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
            List<Player> players = selector.resolve(ctx.getSource());
            if (players == null || players.isEmpty()) {
                sender.sendMessage("Player not found!");
                return 0;
            }
            Player target = players.get(0);
            String host = StringArgumentType.getString(ctx, "ip");
            int port = IntegerArgumentType.getInteger(ctx, "port");
            double x = DoubleArgumentType.getDouble(ctx, "x");
            double y = DoubleArgumentType.getDouble(ctx, "y");
            double z = DoubleArgumentType.getDouble(ctx, "z");

            if (!TransferToken.isValidCoordinate(x, y, z)) {
                sender.sendMessage("Coordinates out of bounds! X and Z must be within ±29,999,984 and Y within -64 to 320.");
                return 0;
            }

            // 0. Ensure connection throttle is disabled for incoming/outgoing transfers
            disableConnectionThrottle();

            // 1. Notify target server over peer bridge to pre-load target coordinates (uses candidate port scanning)
            PeerBridge.sendPreload(
                    host,
                    port,
                    target.getUniqueId(),
                    target.getName(),
                    x, y, z, yaw, pitch
            );

            // 2. Generate and store cryptographic transfer cookie on the player (travels with client)
            try {
                byte[] token = TransferToken.createToken(target.getUniqueId(), host, x, y, z, yaw, pitch, System.currentTimeMillis());
                target.storeCookie(cookieKey, token);
            } catch (Exception ignored) {
            }

            // 3. The native 1.20.5+ transfer packet wrapper in Paper API (1-tick delay to ensure PeerBridge socket arrival)
            FoliaCompat.runDelayedOnPlayer(this, target, () -> {
                if (target.isOnline()) {
                    target.transfer(host, port);
                }
            }, 1L);

            sender.sendMessage(String.format("Admin fast-transferring %s to %s:%d at (%.1f, %.1f, %.1f, yaw: %.1f, pitch: %.1f)",
                    target.getName(), host, port, x, y, z, yaw, pitch));

            return 1;
        } catch (Exception e) {
            sender.sendMessage("Error executing transfer: " + e.getMessage());
            return 0;
        }
    }

    private void disableConnectionThrottle() {
        if (FoliaCompat.IS_FOLIA) {
            return;
        }
        try {
            // 1. Modify in-memory configuration on CraftServer
            Object server = Bukkit.getServer();
            Class<?> serverClass = server.getClass();

            for (Class<?> cl = serverClass; cl != null && cl != Object.class; cl = cl.getSuperclass()) {
                for (java.lang.reflect.Field field : cl.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (org.bukkit.configuration.ConfigurationSection.class.isAssignableFrom(field.getType())) {
                        org.bukkit.configuration.ConfigurationSection cs =
                                (org.bukkit.configuration.ConfigurationSection) field.get(server);
                        if (cs != null) {
                            cs.set("settings.connection-throttle", 0);
                        }
                    }
                    if (field.getName().equalsIgnoreCase("connectionThrottle")) {
                        if (field.getType() == int.class) {
                            field.setInt(server, 0);
                        } else if (field.getType() == long.class) {
                            field.setLong(server, 0L);
                        }
                    }
                }
            }

            // 2. Persist connection-throttle: 0 in bukkit.yml to prevent throttling across reboots
            java.io.File bukkitYml = new java.io.File("bukkit.yml");
            if (bukkitYml.exists()) {
                org.bukkit.configuration.file.YamlConfiguration config =
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(bukkitYml);
                if (config.getInt("settings.connection-throttle", 4000) != 0) {
                    config.set("settings.connection-throttle", 0);
                    config.save(bukkitYml);
                    getLogger().info("Automatically set settings.connection-throttle: 0 in bukkit.yml for seamless transfers.");
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
