package dev.brighten.ac.packet.handler;

import com.google.common.collect.MapMaker;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModernHandler extends HandlerAbstract implements Listener {

    private final Map<String, Channel> channelCache = new HashMap<>();
    private final Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if(!uninjectedChannels.contains(getChannel(event.getPlayer())))
            HandlerAbstract.getHandler().add(event.getPlayer());
    }

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
        if (Anticheat.INSTANCE.isEnabled()) {
            uninjectedChannels.add(channel);
        }

        channel.eventLoop().execute(() -> channel.pipeline().remove(handlerName));
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        getChannel(player).pipeline().writeAndFlush(packet);
    }

    @Override
    public void sendPacket(APlayer player, Object packet) {
        sendPacket(player.getBukkitPlayer(), packet);
    }

    private Channel getChannel(Player player) {
        synchronized (channelCache) {
            return channelCache.computeIfAbsent(player.getName(), name -> MinecraftReflection.getChannel(player));
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

            PacketType type = PacketType
                    .getByPacketId(packetName).orElse(PacketType.UNKNOWN);

            try {
                boolean allowed = Anticheat.INSTANCE.getPacketProcessor().call(player, msg,
                        type);

                if(allowed) {
                    super.channelRead(ctx, msg);
                }
            } catch(Throwable throwable) {
                throwable.printStackTrace();
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            String name = msg.getClass().getName();
            int index = name.lastIndexOf(".");
            String packetName = name.substring(index + 1);

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
        }
    }
}
