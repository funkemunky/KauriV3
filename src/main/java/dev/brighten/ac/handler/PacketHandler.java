package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.out.*;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MovementUtils;
import lombok.val;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayInCustomPayload;
import net.minecraft.server.v1_8_R3.PacketPlayInSteerVehicle;
import net.minecraft.server.v1_8_R3.PacketPlayInTransaction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.*;

public class PacketHandler {

    public void process(APlayer player, PacketType type, Object packetObject) {
        switch (type) {
            case CLIENT_TRANSACTION: {
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

                            if(Math.abs(player.getLagInfo().getLastTransPing()
                                    - player.getLagInfo().getTransPing()) > 1) {
                                player.getLagInfo().getLastPingDrop().reset();
                            }

                            ka.getReceived(player.getBukkitPlayer().getUniqueId()).ifPresent(r -> {
                                r.receivedStamp = currentTimeMillis;
                            });

                            synchronized (player.keepAliveStamps) {
                                List<NormalAction> toRemove = new ArrayList<>();
                                for (NormalAction action : player.keepAliveStamps) {
                                    if(action.stamp > ka.start) continue;

                                    action.action.accept(ka);
                                    toRemove.add(action);
                                }

                                toRemove.forEach(player.keepAliveStamps::remove);
                                toRemove.clear();
                            }
                        });
                        player.getLagInfo().getLastClientTransaction().reset();
                    } else {
                        Optional.ofNullable(player.instantTransaction.remove(packet.b()))
                                .ifPresent(t -> t.two.accept(t.one));
                    }
                }
                break;
            }
            case FLYING: {
                WPacketPlayInFlying packet = (WPacketPlayInFlying) packetObject;

                player.getEntityLocationHandler().onFlying();

                if(player.getMovement().isExcuseNextFlying()) {
                    player.getMovement().setExcuseNextFlying(false);
                    return;
                }

                if(player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_17)
                        && packet.isMoved() && packet.isLooked()
                        && MovementUtils.isSameLocation(new KLocation(packet.getX(), packet.getY(), packet.getZ()),
                        player.getMovement().getTo().getLoc())) {
                    player.getMovement().setExcuseNextFlying(true);
                }

                player.getMovement().process(packet, System.currentTimeMillis());
                break;
            }
            case BLOCK_CHANGE: {
                WPacketPlayOutBlockChange packet = (WPacketPlayOutBlockChange) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
                break;
            }
            case MULTI_BLOCK_CHANGE: {
                WPacketPlayOutMultiBlockChange packet = (WPacketPlayOutMultiBlockChange) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
                break;
            }
            case ENTITY_EFFECT: {
                WPacketPlayOutEntityEffect packet = (WPacketPlayOutEntityEffect) packetObject;

                player.getPotionHandler().onPotionEffect(packet);
                break;
            }
            case VELOCITY: {
                player.runKeepaliveAction(ka -> player.getInfo().getVelocity().reset());
                break;
            }
            case SERVER_POSITION: {
                player.getMovement().addPosition((WPacketPlayOutPosition) packetObject);
                break;
            }
            case ATTACH: {
                WPacketPlayOutAttachEntity packet = (WPacketPlayOutAttachEntity) packetObject;

                if(packet.getHoldingEntityId() != -1) {
                    player.getInfo().setInVehicle(true);
                    player.getInfo().getVehicleSwitch().reset();
                } else {
                    player.getInfo().setInVehicle(false);
                    player.getInfo().getVehicleSwitch().reset();
                }
                break;
            }
            case STEER_VEHICLE: {
                PacketPlayInSteerVehicle packet = (PacketPlayInSteerVehicle) packetObject;

                // Check for isUnmount()
                if(player.getBukkitPlayer().isInsideVehicle() && packet.d()) {
                    player.getInfo().getVehicleSwitch().reset();
                    player.getInfo().setInVehicle(false);
                }

                break;
            }
            case CLIENT_PAYLOAD: {
                PacketPlayInCustomPayload packet = (PacketPlayInCustomPayload) packetObject;

                if(packet.a().equals("Time|Receive")) {
                    PacketDataSerializer serial = packet.b();

                    long serverTime = serial.readLong();
                    long clientReceivedTime = serial.readLong();
                    long currentTime = System.currentTimeMillis();

                    long serverPing = clientReceivedTime - serverTime;
                    long clientToServer = currentTime - clientReceivedTime;
                    long totalFeedback = currentTime - serverTime;

                    player.getBukkitPlayer().sendMessage(String.format("total: %sms client-server: %sms server-client: %sms", totalFeedback, clientToServer, serverPing));
                }
                break;
            }
            case ENTITY_ACTION: {
                WPacketPlayInEntityAction packet = (WPacketPlayInEntityAction) packetObject;

                switch (packet.getAction()) {
                    case START_SNEAKING: {
                        player.getInfo().setSneaking(true);
                        break;
                    }
                    case STOP_SNEAKING: {
                        player.getInfo().setSneaking(false);
                        break;
                    }
                    case START_SPRINTING: {
                        player.getInfo().setSprinting(true);
                        break;
                    }
                    case STOP_SPRINTING: {
                        player.getInfo().setSprinting(false);
                        break;
                    }
                }
                break;
            }
            case ENTITY_TELEPORT: {
                WPacketPlayOutEntityTeleport packet = (WPacketPlayOutEntityTeleport) packetObject;

                player.getEntityLocationHandler().onTeleportSent(packet);
                break;
            }
            case ENTITY:
            case ENTITY_MOVE:
            case ENTITY_LOOK:
            case ENTITY_MOVELOOK: {
                WPacketPlayOutEntity packet = (WPacketPlayOutEntity) packetObject;

                player.getEntityLocationHandler().onRelPosition(packet);
                break;
            }
            case USE_ENTITY: {
                WPacketPlayInUseEntity packet = (WPacketPlayInUseEntity) packetObject;

                Entity target = packet.getEntity(player.getBukkitPlayer().getWorld());

                if(target instanceof LivingEntity) {
                    player.getInfo().setTarget((LivingEntity) target);
                }
                break;
            }
            case BLOCK_DIG: {
                WPacketPlayInBlockPlace packet = (WPacketPlayInBlockPlace) packetObject;

                player.getBlockUpdateHandler().onPlace(packet);
                break;
            }
            case BLOCK_PLACE: {
                WPacketPlayInBlockDig packet = (WPacketPlayInBlockDig) packetObject;

                player.getBlockUpdateHandler().onDig(packet);
                break;
            }
        }

        player.callPacket(packetObject);
    }
}
