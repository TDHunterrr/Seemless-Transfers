package net.seemless.transfers.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.seemless.transfers.SeemLessTransfers;

public class CookieAuthReceiver {

    public static final Identifier COOKIE_ID = Identifier.fromNamespaceAndPath(
            TransferToken.COOKIE_NAMESPACE,
            TransferToken.COOKIE_PATH
    );

    public static void requestAndInterceptCookie(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        try {
            Connection connection = extractConnection(player);
            if (connection == null) return;

            Channel channel = extractNettyChannel(connection);
            if (channel == null) return;

            ChannelPipeline pipeline = channel.pipeline();
            String handlerName = "seemless_cookie_interceptor";

            if (pipeline.get(handlerName) == null) {
                ChannelInboundHandlerAdapter interceptor = new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof ServerboundCookieResponsePacket cookiePacket) {
                            if (COOKIE_ID.equals(cookiePacket.key())) {
                                byte[] payload = cookiePacket.payload();
                                if (payload != null && payload.length > 0) {
                                    TransferToken.LocationTarget loc = TransferToken.parseAndVerifyToken(payload, player.getUUID());
                                    if (loc != null) {
                                        SeemLessTransfers.LOGGER.info("Successfully received transfer cookie for {} over primary game port: ({}, {}, {})",
                                                player.getName().getString(), loc.x(), loc.y(), loc.z());
                                        server.execute(() -> {
                                            SeemLessTransfers.applyTeleport(server, player, loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
                                        });
                                    }
                                }
                                // Drop the packet so vanilla ServerCommonPacketListenerImpl never disconnects the player
                                return;
                            }
                        }
                        super.channelRead(ctx, msg);
                    }
                };

                if (pipeline.get("packet_handler") != null) {
                    pipeline.addBefore("packet_handler", handlerName, interceptor);
                } else {
                    pipeline.addLast(handlerName, interceptor);
                }
            }

            // Request the cookie from the client over the primary game port connection!
            player.connection.send(new ClientboundCookieRequestPacket(COOKIE_ID));
        } catch (Exception e) {
            SeemLessTransfers.LOGGER.warn("Could not register primary-port cookie interceptor for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    private static Connection extractConnection(ServerPlayer player) {
        if (player == null || player.connection == null) return null;
        try {
            for (Class<?> cl = player.connection.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (Connection.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        return (Connection) field.get(player.connection);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Channel extractNettyChannel(Connection connection) {
        if (connection == null) return null;
        try {
            for (Class<?> cl = connection.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (Channel.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        return (Channel) field.get(connection);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
