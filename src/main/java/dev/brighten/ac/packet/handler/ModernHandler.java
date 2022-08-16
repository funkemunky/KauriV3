package dev.brighten.ac.packet.handler;

import com.google.common.collect.MapMaker;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.in.WPacketHandshakingInSetProtocol;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import io.netty.channel.*;
import net.minecraft.server.v1_8_R3.PacketLoginInStart;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ModernHandler extends HandlerAbstract {

    private final Map<String, Channel> channelCache = new HashMap<>();
    private Map<Channel, Integer> protocolLookup = new MapMaker().weakKeys().makeMap();
    private final Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private ChannelInboundHandlerAdapter serverChannelHandler;
    private ChannelInitializer<Channel> beginInitProtocol;
    private ChannelInitializer<Channel> endInitProtocol;

    public ModernHandler() {
        endInitProtocol = new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel channel) throws Exception {
                try {
                    // Stop injecting channels
                    if (!Anticheat.INSTANCE.isEnabled()) {
                        channel.eventLoop().submit(() -> injectChannel(channel));
                    }
                } catch (Exception e) {
                    Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Cannot inject incomming channel " + channel, e);
                }
            }

        };

        // This is executed before Minecraft's channel handler
        beginInitProtocol = new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(endInitProtocol);
            }

        };

        serverChannelHandler = new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Channel channel = (Channel) msg;
                channel.pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }

        };
    }

    @Override
    public void add(Player player) {
        try {
            System.out.println("Adding " + player.getName() + " to packets");
            Channel channel = getChannel(player);

            injectChannel(channel).player = player;
        } catch(IllegalArgumentException e)  {
            System.out.println("Error");
            e.printStackTrace();
        }
    }

    private PacketHandler injectChannel(Channel channel) {
        PacketHandler handler = (PacketHandler) channel.pipeline().get(handlerName);

        if(handler != null) {
            channel.pipeline().remove(handlerName);
        }

        PacketHandler newHandler = new PacketHandler();
        channel.pipeline().addBefore("packet_handler", handlerName, newHandler);
        return newHandler;
    }

    @Override
    public void remove(Player player) {
        Channel channel = getChannel(player);

        if(channel != null) {
            channel.eventLoop().execute(() -> {
                if (channel.pipeline().get(handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
            });
            channelCache.remove(player.getName());
        }
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        if(packet instanceof WPacket) {
            getChannel(player).pipeline().writeAndFlush(((WPacket) packet).getPacket());
        } else getChannel(player).pipeline().writeAndFlush(packet);
    }

    @Override
    public void sendPacket(APlayer player, Object packet) {
        this.sendPacket(player.getBukkitPlayer(), packet);
    }

    @Override
    public int getProtocolVersion(Player player) {
        return protocolLookup.getOrDefault(getChannel(player), -1);
    }

    private Channel getChannel(Player player) {
        synchronized (channelCache) {
            return channelCache.computeIfAbsent(player.getName(), name -> MinecraftReflection.getChannel(player));
        }
    }

    private final class PacketHandler extends ChannelDuplexHandler {
        protected Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String name = msg.getClass().getName();
            int index = name.lastIndexOf(".");
            String packetName = name.substring(index + 1);

            PacketType type = PacketType
                    .getByPacketId(packetName).orElse(PacketType.UNKNOWN);

            if(type == PacketType.LOGIN_START) {
                PacketLoginInStart packet = (PacketLoginInStart) msg;

                channelCache.put(packet.a().getName(), ctx.channel());
            } else if(type == PacketType.LOGIN_HANDSHAKE) {
                WPacketHandshakingInSetProtocol packet = (WPacketHandshakingInSetProtocol) PacketType.processType(type, msg);

                System.out.println("Received handshake");
                if(packet.getProtocol() == WPacketHandshakingInSetProtocol.EnumProtocol.LOGIN) {
                    System.out.println("Setting protocol version number " + packet.getVersionNumber());
                    protocolLookup.put(ctx.channel(), packet.getVersionNumber());
                }
            }

            if(player != null) {
                try {
                    boolean allowed = Anticheat.INSTANCE.getPacketProcessor().call(player, msg,
                            type);

                    if (allowed) {
                        super.channelRead(ctx, msg);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    super.channelRead(ctx, msg);
                }
            } else super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            String name = msg.getClass().getName();
            int index = name.lastIndexOf(".");
            String packetName = name.substring(index + 1);

            if(player != null) {
                try {
                    boolean allowed = Anticheat.INSTANCE.getPacketProcessor().call(player, msg, PacketType
                            .getByPacketId(packetName).orElse(PacketType.UNKNOWN));

                    if (allowed) {
                        super.write(ctx, msg, promise);
                    }
                } catch(Throwable throwable) {
                    throwable.printStackTrace();
                    super.write(ctx, msg, promise);
                }
            } else super.write(ctx, msg, promise);
        }
    }
}
