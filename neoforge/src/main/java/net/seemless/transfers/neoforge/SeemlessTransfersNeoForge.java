package net.seemless.transfers.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.seemless.transfers.command.SlTransferCommand;
import net.seemless.transfers.network.PeerBridge;
import net.seemless.transfers.network.PreloadTarget;

@Mod("seemless_transfers")
@EventBusSubscriber(modid = "seemless_transfers")
public class SeemlessTransfersNeoForge {

    public SeemlessTransfersNeoForge() {
        PeerBridge.startServer(
                PeerBridge.DEFAULT_PEER_PORT,
                payload -> {}
        );
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SlTransferCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            PreloadTarget target = PeerBridge.getAndConsumeTarget(player.getUUID());
            if (target != null) {
                player.teleportTo(target.x(), target.y(), target.z());
                player.setYRot(target.yaw());
                player.setXRot(target.pitch());
            }
        }
    }
}
