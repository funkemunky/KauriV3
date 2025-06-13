package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.out.*;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.math.IntVector;
import lombok.val;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayInCustomPayload;
import net.minecraft.server.v1_8_R3.PacketPlayInSteerVehicle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class PacketHandler {

    public boolean process(APlayer player, PacketType type, Object packetObject) {
        long timestamp = System.currentTimeMillis();

        switch (type) {
            case CLIENT_TRANSACTION -> {
                WPacketPlayInTransaction packet = (WPacketPlayInTransaction) packetObject;

                if (packet.getId() == 0) {
                    if (Anticheat.INSTANCE.getKeepaliveProcessor().keepAlives.get(packet.getAction()) != null) {
                        Anticheat.INSTANCE.getKeepaliveProcessor().addResponse(player, packet.getAction());

                        val optional = Anticheat.INSTANCE.getKeepaliveProcessor().getResponse(player);

                        int current = Anticheat.INSTANCE.getKeepaliveProcessor().tick;

                        optional.ifPresent(ka -> {
                            player.addPlayerTick();

                            player.getLagInfo().setLastTransPing(player.getLagInfo().getTransPing());
                            player.getLagInfo().setTransPing(current - ka.id);

                            if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)
                                    && player.getMovement().getLastFlying().isPassed(1)) {
                                player.getMovement().runPositionHackFix();
                            }

                            if (!player.instantTransaction.isEmpty()) {
                                synchronized (player.instantTransaction) {
                                    Deque<Short> toRemove = new LinkedList<>();
                                    player.instantTransaction.forEach((key, tuple) -> {
                                        if ((timestamp - tuple.one.getStamp())
                                                > player.getLagInfo().getTransPing() * 52L + 750L) {
                                            tuple.two.accept(tuple.one);
                                            toRemove.add(key);
                                        }
                                    });
                                    Short key;
                                    while ((key = toRemove.poll()) != null) {
                                        player.instantTransaction.remove(key);
                                    }
                                }
                            }

                            if (Math.abs(player.getLagInfo().getLastTransPing()
                                    - player.getLagInfo().getTransPing()) > 1) {
                                player.getLagInfo().getLastPingDrop().reset();
                            }

                            ka.getReceived(player.getBukkitPlayer().getUniqueId())
                                    .ifPresent(r -> r.receivedStamp = timestamp);

                            synchronized (player.keepAliveStamps) {
                                List<NormalAction> toRemove = new ArrayList<>();
                                for (NormalAction action : player.keepAliveStamps) {
                                    if (action.stamp > ka.id) continue;

                                    action.action.accept(ka);
                                    toRemove.add(action);
                                }

                                toRemove.forEach(player.keepAliveStamps::remove);
                                toRemove.clear();
                            }
                        });
                        player.getLagInfo().getLastClientTransaction().reset();
                    }
                    Optional.ofNullable(player.instantTransaction.remove(packet.getAction()))
                            .ifPresent(t -> t.two.accept(t.one));
                }
            }
            case SERVER_ABILITIES -> {
                WPacketPlayOutAbilities packet = (WPacketPlayOutAbilities) packetObject;

                player.getInfo().getLastAbilities().reset();

                player.runInstantAction(ia -> {
                    if (!ia.isEnd()) {
                        player.getInfo().getPossibleCapabilities().add(packet.getCapabilities());
                    } else if (player.getInfo().getPossibleCapabilities().size() > 1) {
                        player.getInfo().getPossibleCapabilities().clear();
                    }
                });
            }
            case FLYING -> {
                WPacketPlayInFlying packet = (WPacketPlayInFlying) packetObject;

                if (player.getMovement().isExcuseNextFlying()) {
                    player.getMovement().setExcuseNextFlying(false);
                    return false;
                }

                if (timestamp - player.getLagInfo().getLastFlying() <= 15) {
                    player.getLagInfo().getLastPacketDrop().reset();
                }

                player.getLagInfo().setLastFlying(timestamp);

                player.getEntityLocationHandler().onFlying();

                if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_17)
                        && packet.isMoved() && packet.isLooked()
                        && MovementUtils.isSameLocation(new KLocation(packet.getX(), packet.getY(), packet.getZ()),
                        player.getMovement().getTo().getLoc())) {
                    player.getMovement().setExcuseNextFlying(true);
                }

                player.getMovement().process(packet);
            }
            case BLOCK_CHANGE -> {
                WPacketPlayOutBlockChange packet = (WPacketPlayOutBlockChange) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
            }
            case MULTI_BLOCK_CHANGE -> {
                WPacketPlayOutMultiBlockChange packet = (WPacketPlayOutMultiBlockChange) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
            }
            case MAP_CHUNK -> {
                WPacketPlayOutMapChunk packet = (WPacketPlayOutMapChunk) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
            }
            case MAP_CHUNK_BULK -> {
                WPacketPlayOutMapChunkBulk packet = (WPacketPlayOutMapChunkBulk) packetObject;

                player.getBlockUpdateHandler().runUpdate(packet);
            }
            case ENTITY_EFFECT -> {
                WPacketPlayOutEntityEffect packet = (WPacketPlayOutEntityEffect) packetObject;

                player.getPotionHandler().onPotionEffect(packet);
            }
            case EXPLOSION -> {
                WPacketPlayOutExplosion packet = (WPacketPlayOutExplosion) packetObject;

                Vector velocity = packet.getEntityPush().toBukkitVector();

                player.getInfo().getVelocityHistory().add(velocity);
                player.getInfo().setDoingVelocity(true);

                player.runInstantAction(ka -> {
                    if (!ka.isEnd()) {
                        player.getVelocityHandler().onPre(packet);
                    } else player.getVelocityHandler().onPost(packet);
                    if (ka.isEnd() && player.getInfo().getVelocityHistory().contains(velocity)) {
                        player.getInfo().setDoingVelocity(false);
                        player.getInfo().getVelocity().reset();
                        synchronized (player.getInfo().getVelocityHistory()) {
                            player.getInfo().getVelocityHistory().remove(velocity);
                        }
                    }
                });
                player.runKeepaliveAction(ka -> {
                    if (player.getInfo().getVelocityHistory().contains(velocity))
                        player.getOnVelocityTasks().forEach(task -> task.accept(velocity));
                }, 1);
            }
            case VELOCITY -> {
                WPacketPlayOutEntityVelocity packet = (WPacketPlayOutEntityVelocity) packetObject;

                if (packet.getEntityId() == player.getBukkitPlayer().getEntityId()) {
                    Vector velocity = new Vector(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
                    player.getInfo().getVelocityHistory().add(velocity);
                    player.getInfo().setDoingVelocity(true);

                    player.runInstantAction(ka -> {
                        if (!ka.isEnd()) {
                            player.getVelocityHandler().onPre(packet);
                        } else player.getVelocityHandler().onPost(packet);
                        if (ka.isEnd() && player.getInfo().getVelocityHistory().contains(velocity)) {
                            player.getInfo().setDoingVelocity(false);
                            player.getInfo().getVelocity().reset();
                            synchronized (player.getInfo().getVelocityHistory()) {
                                player.getInfo().getVelocityHistory().remove(velocity);
                            }
                        }
                    });
                    player.runKeepaliveAction(ka -> {
                        if (player.getInfo().getVelocityHistory().contains(velocity))
                            player.getOnVelocityTasks().forEach(task -> task.accept(velocity));
                    }, 1);
                }
            }
            case RESPAWN -> {
                if (player.getPlayerVersion().isBelow(ProtocolVersion.V1_14)) {
                    player.runKeepaliveAction(k -> player.getBukkitPlayer().setSprinting(false), 1);
                }
                player.runKeepaliveAction(ka -> player.getInfo().lastRespawn.reset());
            }
            case SERVER_POSITION -> player.getMovement().addPosition((WPacketPlayOutPosition) packetObject);
            case ATTACH -> {
                WPacketPlayOutAttachEntity packet = (WPacketPlayOutAttachEntity) packetObject;

                if (packet.getHoldingEntityId() != -1) {
                    player.getInfo().setInVehicle(true);
                    player.getInfo().getVehicleSwitch().reset();
                } else {
                    player.getInfo().setInVehicle(false);
                    player.getInfo().getVehicleSwitch().reset();
                }
            }
            case STEER_VEHICLE -> {
                PacketPlayInSteerVehicle packet = (PacketPlayInSteerVehicle) packetObject;

                // Check for isUnmount()
                if (player.getBukkitPlayer().isInsideVehicle() && packet.d()) {
                    player.getInfo().getVehicleSwitch().reset();
                    player.getInfo().setInVehicle(false);
                }

            }
            case CLIENT_PAYLOAD -> {
                PacketPlayInCustomPayload packet = (PacketPlayInCustomPayload) packetObject;

                if (packet.a().equals("Time|Receive")) {
                    PacketDataSerializer serial = packet.b();

                    long serverTime = serial.readLong();
                    long clientReceivedTime = serial.readLong();

                    long serverPing = clientReceivedTime - serverTime;
                    long clientToServer = timestamp - clientReceivedTime;
                    long totalFeedback = timestamp - serverTime;

                    player.getBukkitPlayer().sendMessage(String.format("total: %sms client-server: %sms server-client: %sms", totalFeedback, clientToServer, serverPing));
                }
            }
            case ENTITY_DESTROY -> {
                WPacketPlayOutEntityDestroy packet = (WPacketPlayOutEntityDestroy) packetObject;

                player.getEntityLocationHandler().onEntityDestroy(packet);
            }
            case ENTITY_ACTION -> {
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
            }
            case ENTITY_TELEPORT -> {
                WPacketPlayOutEntityTeleport packet = (WPacketPlayOutEntityTeleport) packetObject;

                player.getEntityLocationHandler().onTeleportSent(packet);
            }
            case ENTITY, ENTITY_MOVE, ENTITY_LOOK, ENTITY_MOVELOOK -> {
                WPacketPlayOutEntity packet = (WPacketPlayOutEntity) packetObject;

                player.getEntityLocationHandler().onRelPosition(packet);
            }
            case USE_ENTITY -> {
                WPacketPlayInUseEntity packet = (WPacketPlayInUseEntity) packetObject;

                FakeMob mob = Anticheat.INSTANCE.getFakeTracker().getEntityById(packet.getEntityId());

                if (packet.getAction() == WPacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                    if (mob != null) {
                        player.getEntityLocationHandler().getTargetOfFakeMob(mob.getEntityId())
                                .ifPresent(targetId -> {
                                    player.getEntityLocationHandler().removeFakeMob(targetId);
                                    player.getInfo().lastFakeBotHit.reset();
                                });
                        if (player.getMob().getEntityId() == packet.getEntityId()) {
                            player.getInfo().botAttack.reset();
                        }
                    } else {
                        Entity target = packet.getEntity(player.getBukkitPlayer().getWorld());

                        if (target instanceof LivingEntity) {
                            if (player.getInfo().lastFakeBotHit.isPassed(400) && Math.random() > 0.9) {
                                player.getEntityLocationHandler().canCreateMob.add(target.getEntityId());
                            }
                            player.getInfo().setTarget((LivingEntity) target);
                        }
                    }
                    player.getInfo().lastAttack.reset();
                }
            }
            case ARM_ANIMATION -> {
                long delta = timestamp - player.getInfo().lastArmSwing;

                player.getInfo().cps.add(1000D / delta, timestamp);
                player.getInfo().lastArmSwing = timestamp;
            }
            case BLOCK_PLACE -> {
                WPacketPlayInBlockPlace packet = (WPacketPlayInBlockPlace) packetObject;

                IntVector pos = packet.getBlockPos();
                ItemStack stack = packet.getItemStack();

                player.getInfo().getLastBlockPlace().reset();

                // Used item
                if (pos.getX() == -1 && (pos.getY() == 255 | pos.getY() == -1) && pos.getZ() == -1
                        && stack != null
                        && BlockUtils.isUsable(stack.getType())) {
                    player.getInfo().getLastUseItem().reset();
                }

                player.getBlockUpdateHandler().onPlace(packet);
            }
            case BLOCK_DIG -> {
                WPacketPlayInBlockDig packet = (WPacketPlayInBlockDig) packetObject;

                player.getInfo().getLastBlockDig().reset();
                player.getBlockUpdateHandler().onDig(packet);
            }
            case CLIENT_COMMAND -> {
                WPacketPlayInClientCommand packet = (WPacketPlayInClientCommand) packetObject;

                if (packet.getCommand() == WPacketPlayInClientCommand.WrappedEnumClientCommand
                        .OPEN_INVENTORY_ACHIEVEMENT) {
                    player.getInfo().setInventoryOpen(true);
                    player.getInfo().lastInventoryOpen.reset();
                    return true;
                }
            }
            case CLIENT_CLOSE_WINDOW -> player.getInfo().setInventoryOpen(false);
            case SERVER_CLOSE_WINDOW -> player.runKeepaliveAction(ka -> player.getInfo().setInventoryOpen(false));
            case SERVER_OPEN_WINDOW -> player.runKeepaliveAction(ka -> {
                player.getInfo().setInventoryOpen(true);
                player.getInfo().lastInventoryOpen.reset();
            });
        }

        if(player.sniffing) {
            if(type != PacketType.UNKNOWN) {
                player.sniffedPackets.add("[" + Anticheat.INSTANCE.getKeepaliveProcessor().tick + "] " + type.name()
                        + ": " + packetObject.toString());
            } else {
                player.sniffedPackets.add("[" + Anticheat.INSTANCE.getKeepaliveProcessor().tick + "] (UNKNOWN) "
                        + packetObject.getClass().getSimpleName() + ": " + packetObject);
            }
        }

        boolean cancelled = player.getCheckHandler().callSyncPacket(packetObject, timestamp);

        // Post flying settings
        if(type.equals(PacketType.FLYING)) {
            player.getVelocityHandler().onFlyingPost((WPacketPlayInFlying)packetObject);
            player.getInfo().lsneaking = player.getInfo().sneaking;
        }

        return cancelled;
    }
}
