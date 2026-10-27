package net.seemless.transfers;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeemLessTransfers implements ModInitializer {
	public static final String MOD_ID = "seemless-transfers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Seemless Transfers (Fabric) initialized!");

		// Register command
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
				net.seemless.transfers.command.SlTransferCommand::register
		);

		// Start peer bridge bound to the actual dedicated server port
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			int port = server.getPort() > 0 ? server.getPort() : 25565;
			net.seemless.transfers.network.PeerBridge.startServer(
					port,
					payload -> {
						LOGGER.info("Received fast-transfer preload request: {}", payload);
						try {
							String[] parts = payload.split(":");
							if (parts.length >= 7) {
								java.util.UUID uuid = java.util.UUID.fromString(parts[0]);
								String playerName = parts[1];
								double x = Double.parseDouble(parts[2]);
								double y = Double.parseDouble(parts[3]);
								double z = Double.parseDouble(parts[4]);
								float yaw = Float.parseFloat(parts[5]);
								float pitch = Float.parseFloat(parts[6]);
								server.execute(() -> {
									net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
									if (p == null && playerName != null && !playerName.isEmpty()) {
										p = server.getPlayerList().getPlayerByName(playerName);
									}
									if (p != null) {
										applyTeleport(server, p, x, y, z, yaw, pitch);
									}
								});
							}
						} catch (Exception e) {
							LOGGER.error("Failed to parse preload request: " + payload, e);
						}
					}
			);
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			net.seemless.transfers.network.ServerPinger.shutdown();
			net.seemless.transfers.network.PeerBridge.stopServer();
		});

		// Teleport player upon connection
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			net.minecraft.server.level.ServerPlayer player = handler.getPlayer();

			// 1. Request client cookie directly over the primary game port (works on all shared hosting providers)
			net.seemless.transfers.network.CookieAuthReceiver.requestAndInterceptCookie(player, server);

			// 2. Also check instant PeerBridge socket memory cache if direct socket was reachable
			server.execute(() -> {
				if (player.hasDisconnected()) return;
				net.seemless.transfers.network.PeerBridge.PreloadTarget target =
						net.seemless.transfers.network.PeerBridge.getAndConsumeTarget(player.getUUID(), player.getName().getString());
				if (target != null) {
					applyTeleport(server, player, target.x(), target.y(), target.z(), target.yaw(), target.pitch());
				}
			});
		});
	}

	public static void applyTeleport(net.minecraft.server.MinecraftServer server,
									 net.minecraft.server.level.ServerPlayer player,
									 double x, double y, double z, float yaw, float pitch) {
		if (player == null || player.hasDisconnected()) return;

		// 1. Asynchronously / synchronously preload destination chunk for smooth arrival
		net.minecraft.server.level.ServerLevel level = player.level() instanceof net.minecraft.server.level.ServerLevel sl
				? sl : server.overworld();
		int chunkX = ((int) x) >> 4;
		int chunkZ = ((int) z) >> 4;
		level.getChunkSource().getChunk(chunkX, chunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);

		// 2. Perform direct teleportation
		player.teleportTo(level, x, y, z, java.util.Collections.emptySet(), yaw, pitch, false);
		player.connection.teleport(x, y, z, yaw, pitch);
		player.setYHeadRot(yaw);
		LOGGER.info("Teleported transferred player {} to ({}, {}, {}) with yaw {} pitch {}",
				player.getName().getString(), x, y, z, yaw, pitch);

		// 3. Confirm position after terrain download handshake (150ms delay) to prevent vanilla client spawn override
		java.util.concurrent.CompletableFuture.delayedExecutor(150, java.util.concurrent.TimeUnit.MILLISECONDS).execute(() -> {
			server.execute(() -> {
				if (!player.hasDisconnected()) {
					player.connection.teleport(x, y, z, yaw, pitch);
					player.setYHeadRot(yaw);
				}
			});
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
