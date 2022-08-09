package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityEffect;
import lombok.val;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayInTransaction;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

public class PacketHandler {
    public void process(APlayer player, Packet packetObject) {
        if(packetObject instanceof PacketPlayInTransaction) {
            long currentTimeMillis = System.currentTimeMillis();
            PacketPlayInTransaction packet = (PacketPlayInTransaction) packetObject;

            if(packet.a() == 0) {
                if(Anticheat.INSTANCE.getKeepaliveProcessor().keepAlives.getIfPresent(packet.b()) != null) {
                    Anticheat.INSTANCE.getKeepaliveProcessor().addResponse(player, packet.b());

                    val optional = Anticheat.INSTANCE.getKeepaliveProcessor().getResponse(player);

                    int current = Anticheat.INSTANCE.getKeepaliveProcessor().tick;

                    optional.ifPresent(ka -> {
                        player.addPlayerTick();

                        player.getLagInfo().setLastTransPing(player.getLagInfo().getTransPing());
                        player.getLagInfo().setTransPing(current - ka.start);

                        if(player.instantTransaction.size() > 0) {
                            synchronized (player.instantTransaction) {
                                Deque<Short> toRemove = new LinkedList<>();
                                player.instantTransaction.forEach((key, tuple) -> {
                                    if((currentTimeMillis - tuple.one.getStamp())
                                            > player.getLagInfo().getTransPing() * 52L + 750L) {
                                        tuple.two.accept(tuple.one);
                                        toRemove.add(key);
                                    }
                                });
                                Short key = null;
                                while((key = toRemove.poll()) != null) {
                                    player.instantTransaction.remove(key);
                                }
                            }
                        }

                        if(Math.abs(player.getLagInfo().getLastTransPing() - player.getLagInfo().getTransPing()) > 1) {
                            player.getLagInfo().getLastPingDrop().reset();
                        }

                        ka.getReceived(player.getBukkitPlayer().getUniqueId()).ifPresent(r -> {
                            r.receivedStamp = currentTimeMillis;
                        });

                        for (NormalAction action : player.keepAliveStamps) {
                            if(action.stamp > ka.start) continue;

                            action.action.accept(ka);
                            player.keepAliveStamps.remove(action);
                        }
                    });
                    player.getLagInfo().getLastClientTransaction().reset();
                } else {
                    Optional.ofNullable(player.instantTransaction.remove(packet.b()))
                            .ifPresent(t -> t.two.accept(t.one));
                }
            }
        }

        player.callPacket(packetObject);
    }

    public void process(APlayer player, WPacket packetObject) {

        if(packetObject instanceof WPacketPlayInFlying) {
            WPacketPlayInFlying packet = (WPacketPlayInFlying) packetObject;

            player.getMovement().process(packet, System.currentTimeMillis());
        } else if(packetObject instanceof WPacketPlayOutEntityEffect) {
            WPacketPlayOutEntityEffect packet = (WPacketPlayOutEntityEffect) packetObject;

            player.getPotionHandler().onPotionEffect(packet);
        }

        player.callPacket(packetObject);
    }
}
