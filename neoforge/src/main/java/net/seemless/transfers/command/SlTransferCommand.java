package net.seemless.transfers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.seemless.transfers.network.PeerBridge;
import net.seemless.transfers.network.TransferToken;

public class SlTransferCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("sl")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("ip", StringArgumentType.string())
                                .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg(TransferToken.MIN_XZ, TransferToken.MAX_XZ))
                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg(TransferToken.MIN_Y, TransferToken.MAX_Y))
                                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg(TransferToken.MIN_XZ, TransferToken.MAX_XZ))
                                                                .executes(context -> {
                                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                                    int port = IntegerArgumentType.getInteger(context, "port");
                                                                    double x = DoubleArgumentType.getDouble(context, "x");
                                                                    double y = DoubleArgumentType.getDouble(context, "y");
                                                                    double z = DoubleArgumentType.getDouble(context, "z");
                                                                    return transfer(context, player, port, x, y, z, 0.0f, 0.0f);
                                                                })
                                                                .then(Commands.argument("yaw", DoubleArgumentType.doubleArg(-180.0, 180.0))
                                                                        .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(-90.0, 90.0))
                                                                                .executes(context -> {
                                                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                                                    int port = IntegerArgumentType.getInteger(context, "port");
                                                                                    double x = DoubleArgumentType.getDouble(context, "x");
                                                                                    double y = DoubleArgumentType.getDouble(context, "y");
                                                                                    double z = DoubleArgumentType.getDouble(context, "z");
                                                                                    float yaw = (float) DoubleArgumentType.getDouble(context, "yaw");
                                                                                    float pitch = (float) DoubleArgumentType.getDouble(context, "pitch");
                                                                                    return transfer(context, player, port, x, y, z, yaw, pitch);
                                                                                }))))))))));
    }

    private static int transfer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                ServerPlayer player, int port, double x, double y, double z, float yaw, float pitch) {
        try {
            final String ip = StringArgumentType.getString(context, "ip");

            if (!TransferToken.isValidCoordinate(x, y, z)) {
                context.getSource().sendFailure(Component.literal(
                        "Coordinates out of bounds! X and Z must be within ±29,999,984 and Y within -64 to 320."));
                return 0;
            }

            // 1. Notify target server over peer bridge to pre-load specific coordinates (if directly reachable)
            PeerBridge.sendPreload(ip, port, player.getUUID(), player.getName().getString(), x, y, z, yaw, pitch);

            // 2. Generate and store cryptographic transfer cookie on the client (Minecraft 1.20.5+ native)
            byte[] token = TransferToken.createToken(player.getUUID(), ip, x, y, z, yaw, pitch, System.currentTimeMillis());
            Identifier cookieId = Identifier.fromNamespaceAndPath(
                    TransferToken.COOKIE_NAMESPACE,
                    TransferToken.COOKIE_PATH
            );
            player.connection.send(new ClientboundStoreCookiePacket(cookieId, token));

            // 3. Send native transfer packet
            player.connection.send(new ClientboundTransferPacket(ip, port));
            context.getSource().sendSuccess(() -> Component.literal(
                    "Admin fast-transferring " + player.getName().getString() + " to " + ip + ":" + port +
                    " at (" + String.format("%.1f, %.1f, %.1f, yaw: %.1f, pitch: %.1f", x, y, z, yaw, pitch) + ")"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to initiate transfer: " + e.getMessage()));
            return 0;
        }
    }
}
