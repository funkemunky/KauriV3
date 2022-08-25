package dev.brighten.ac.packet.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.AllArgsConstructor;
import net.minecraft.server.v1_7_R4.NetworkManager;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.util.com.google.common.collect.MapMaker;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LegacyHandler extends HandlerAbstract {

    private final Map<String, Channel> channelCache = new HashMap<>();
    private Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private static WrappedField fieldChannel = new WrappedClass(NetworkManager.class).getFieldByType(Channel.class, 0);

    @Override
    public void add(Player player) {
        try {
            Channel channel = getChannel(player);

            PacketHandler handler = (PacketHandler) channel.pipeline().get(handlerName);

            if(handler == null) {
                handler = new PacketHandler(player);

                ChannelHandlerContext context = channel.pipeline().context("packet_handler");
                if(context != null) {
                    channel.pipeline().addBefore("packet_handler", handlerName, handler);
                    uninjectedChannels.remove(channel);
                } else uninjectedChannels.add(channel);
            }
        } catch(IllegalArgumentException e)  {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Player player) {
        Channel channel = getChannel(player);
        if(Anticheat.INSTANCE.isEnabled()) {
            uninjectedChannels.add(channel);
        }

        channel.eventLoop().execute(() -> channel.pipeline().remove(handlerName));
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
        return -1;
    }

    private Channel getChannel(Player player) {
        synchronized (channelCache) {
            return channelCache.computeIfAbsent(player.getName(), name -> {
                PlayerConnection connection = ((CraftPlayer)player).getHandle().playerConnection;

                return fieldChannel.get(connection.networkManager);
            });
        }
    }

    @AllArgsConstructor
    private static class PacketHandler extends ChannelDuplexHandler {
        private Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String name = msg.getClass().getName();
            int index = name.lastIndexOf(".");
            String packetName = name.substring(index + 1);

            Object packet = Anticheat.INSTANCE.getPacketProcessor().call(player, msg, PacketType
                    .getByPacketId(packetName).orElse(PacketType.UNKNOWN));

            if(packet != null) {
                super.channelRead(ctx, packet);
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            String name = msg.getClass().getName();
            int index = name.lastIndexOf(".");
            String packetName = name.substring(index + 1);
            Object packet = Anticheat.INSTANCE.getPacketProcessor().call(player, msg, PacketType
                    .getByPacketId(packetName).orElse(PacketType.UNKNOWN));

            if(packet != null) {
                super.write(ctx, packet, promise);
            }
        }
    }
}
