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
								double x = Double.parseDouble(parts[2]);
								double y = Double.parseDouble(parts[3]);
								double z = Double.parseDouble(parts[4]);
								float yaw = Float.parseFloat(parts[5]);
								float pitch = Float.parseFloat(parts[6]);
								server.execute(() -> {
									net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
									if (p != null) {
										p.teleportTo(p.level(), x, y, z, java.util.Collections.emptySet(), yaw, pitch, false);
										LOGGER.info("Teleported already-joined player {} to ({}, {}, {})",
												p.getName().getString(), x, y, z);
									}
								});
							}
						} catch (Exception ignored) {
						}
					}
			);
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			net.seemless.transfers.network.PeerBridge.stopServer();
		});

		// Teleport player upon connection
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			net.minecraft.server.level.ServerPlayer player = handler.getPlayer();
			server.execute(() -> {
				if (player.hasDisconnected()) return;
				net.seemless.transfers.network.PeerBridge.PreloadTarget target =
						net.seemless.transfers.network.PeerBridge.getAndConsumeTarget(player.getUUID());
				if (target != null) {
					player.teleportTo(player.level(), target.x(), target.y(), target.z(),
							java.util.Collections.emptySet(), target.yaw(), target.pitch(), false);
					LOGGER.info("Teleported incoming transferred player {} to ({}, {}, {}) with yaw {} pitch {}",
							player.getName().getString(), target.x(), target.y(), target.z(), target.yaw(), target.pitch());
				}
			});
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
