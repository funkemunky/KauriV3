package dev.brighten.ac.packet.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.login.WPacketHandshakingInSetProtocol;
import dev.brighten.ac.utils.reflections.impl.CraftReflection;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import io.netty.channel.*;
import net.minecraft.server.v1_8_R3.PacketLoginInStart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

public class ModernHandler extends HandlerAbstract {

    private final Map<String, Channel> channelCache = new HashMap<>();
    private final Map<Channel, Integer> protocolLookup = new MapMaker().weakKeys().makeMap();

    private final ChannelInboundHandlerAdapter serverChannelHandler;
    private final ChannelInitializer<Channel> beginInitProtocol;
    private final ChannelInitializer<Channel> endInitProtocol;
    private final List<Channel> serverChannels = Lists.newArrayList();

    public ModernHandler() {
        endInitProtocol = new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel channel) {
                try {
                    // Stop injecting channels
                    if (Anticheat.INSTANCE.isEnabled()) {
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
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(endInitProtocol);
            }

        };

        serverChannelHandler = new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                Channel channel = (Channel) msg;
                channel.pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }

        };

        Anticheat.INSTANCE.getRunUtils().task(() -> {
            Object mcServer = CraftReflection.getMinecraftServer();
            Object serverConnection = MinecraftReflection.getServerConnection(mcServer);
            boolean looking = true;

            // We need to synchronize against this list
            for (Method m : mcServer.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType()
                        .isAssignableFrom(MinecraftReflection.serverConnection.getParent())) {
                    try {
                        Object result = m.invoke(mcServer);
                        if (result != null) serverConnection = result;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            for (int i = 0; looking; i++) {
                List<Object> list =  new WrappedClass(serverConnection.getClass()).getFieldByType(List.class, i)
                        .get(serverConnection);

                for (Object item : list) {
                    //if (!ChannelFuture.class.isInstance(item))
                    //	break;

                    // Channel future that contains the server connection
                    Channel serverChannel = ((ChannelFuture) item).channel();

                    serverChannels.add(serverChannel);
                    serverChannel.pipeline().addFirst(serverChannelHandler);
                    Bukkit.getLogger().info("Server channel handler injected (" + serverChannel + ")");
                    looking = false;
                }
            }
        });
    }

    @Override
    public void add(Player player) {
        try {
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
            uninjectChannel(channel);
            channelCache.remove(player.getName());
        }
    }

    private void uninjectChannel(Channel channel) {
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
        });
    }

    @Override
    public void shutdown() {
        Bukkit.getOnlinePlayers().forEach(this::remove);
        for (Channel serverChannel : serverChannels) {
            serverChannel.eventLoop().execute(() -> {
                final ChannelPipeline pipeline = serverChannel.pipeline();
                try {
                    pipeline.remove(serverChannelHandler);
                } catch (NoSuchElementException e) {
                    // That's fine
                }
            });
        }
        serverChannels.clear();
    }

    @Override
    public void sendPacketSilently(Player player, Object packet) {
        if(packet instanceof WPacket) {
            getChannel(player).pipeline().writeAndFlush((new SilentObject(((WPacket) packet).getPacket())));
        } else getChannel(player).pipeline().writeAndFlush(new SilentObject(packet));
    }

    @Override
    public void sendPacketSilently(APlayer player, Object packet) {
        this.sendPacketSilently(player.getBukkitPlayer(), packet);
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        if(packet instanceof WPacket) {
            getChannel(player).pipeline().writeAndFlush(((WPacket) packet).getPacket());
        } else getChannel(player).pipeline().writeAndFlush(packet);
    }

    @Override
    public void sendPacket(APlayer player, Object packet) {
        sendPacket(player.getBukkitPlayer(), packet);
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
        private Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            if(msg instanceof SilentObject) {
                super.channelRead(ctx, ((SilentObject)msg).packet);
                return;
            }

            PacketType type = HandlerAbstract.getPacketType(msg);

            if(type == PacketType.LOGIN_START) {
                PacketLoginInStart packet = (PacketLoginInStart) msg;

                channelCache.put(packet.a().getName(), ctx.channel());
            } else if(type == PacketType.LOGIN_HANDSHAKE) {
                WPacketHandshakingInSetProtocol packet = (WPacketHandshakingInSetProtocol) PacketType.processType(type, msg);

                if(packet.getProtocol() == WPacketHandshakingInSetProtocol.EnumProtocol.LOGIN) {
                    protocolLookup.put(ctx.channel(), packet.getVersionNumber());
                }
            }

            if(player != null && Anticheat.INSTANCE.getPacketProcessor() != null) {
                try {
                    Object returnedObject = Anticheat.INSTANCE.getPacketProcessor().call(player, msg,
                            type);

                    if (returnedObject != null) {
                        super.channelRead(ctx, returnedObject);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    try {
                        super.channelRead(ctx, msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

            if(msg instanceof SilentObject) {
                super.write(ctx, ((SilentObject)msg).packet, promise);
                return;
            }

            if(player != null) {
                try {
                    Object returnedObject = Anticheat.INSTANCE.getPacketProcessor().call(player, msg,
                            HandlerAbstract.getPacketType(msg));

                    if (returnedObject != null) {
                        super.write(ctx, returnedObject, promise);
                    }
                } catch(Throwable throwable) {
                    throwable.printStackTrace();
                    super.write(ctx, msg, promise);
                }
            } else super.write(ctx, msg, promise);
        }
    }
}
