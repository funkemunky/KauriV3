package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.info.PlayerInput;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.packet.PlayerCapabilities;
import dev.brighten.ac.packet.TransactionClientWrapper;
import dev.brighten.ac.packet.WPacketPlayOutEntity;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MovementUtils;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.val;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

public class PacketHandler {

    public boolean processReceive(APlayer player, PacketReceiveEvent event) {
        long timestamp = System.currentTimeMillis();
        Object wrapped;
        if (event.getPacketType().equals(PacketType.Play.Client.PONG)
                || event.getPacketType().equals(PacketType.Play.Client.WINDOW_CONFIRMATION)) {
            TransactionClientWrapper packet = new TransactionClientWrapper(event);

            wrapped = packet;

            if(packet.getId() == 0) {
                if (Anticheat.INSTANCE.getKeepaliveProcessor().keepAlives.get(packet.getAction()) != null) {
                    Anticheat.INSTANCE.getKeepaliveProcessor().addResponse(player, packet.getAction());

                    val optional = Anticheat.INSTANCE.getKeepaliveProcessor().getResponse(player);

                    int current = Anticheat.INSTANCE.getKeepaliveProcessor().tick;

                    optional.ifPresent(ka -> {
                        player.addPlayerTick();

                        player.getLagInfo().setLastTransPing(player.getLagInfo().getTransPing());
                        player.getLagInfo().setTransPing(current - ka.id);

                        if (player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                                && player.getPlayerVersion().isOlderThanOrEquals(ClientVersion.V_1_21)
                                && player.getMovement().getLastFlying().isPassed(2)) {
                            player.getMovement().runPositionHackFix();
                        }

                        if (!player.instantTransaction.isEmpty()) {
                            synchronized (player.instantTransaction) {
                                var iterator = player.instantTransaction.keySet().iterator();

                                while(iterator.hasNext()) {
                                    Short key = iterator.next();

                                    if (key > ka.id) {
                                        continue;
                                    }

                                    var tuple = player.instantTransaction.get(key);

                                    if ((timestamp - tuple.one.getStamp())
                                            > player.getLagInfo().getTransPing() * 52L + 750L) {
                                        tuple.two.accept(tuple.one);
                                        iterator.remove();
                                    }
                                }
                            }
                        }

                        if (Math.abs(player.getLagInfo().getLastTransPing()
                                - player.getLagInfo().getTransPing()) > 1) {
                            player.getLagInfo().getLastPingDrop().reset();
                        }

                        ka.getReceived(player.getBukkitPlayer().getUniqueId())
                                .ifPresent(r -> r.receivedStamp = timestamp);

                        synchronized (player.keepAliveLock) {
                            player.keepAliveStamps.removeIf(action -> {
                                if(action.stamp > ka.id) {
                                    return false;
                                }

                                action.action.accept(ka);
                                return true;
                            });
                        }
                    });
                    player.getLagInfo().getLastClientTransaction().reset();
                }
                Optional.ofNullable(player.instantTransaction.remove(packet.getAction()))
                        .ifPresent(t -> t.two.accept(t.one));
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.PLAYER_INPUT)) {
            WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);

            wrapped = packet;

            player.getInfo().setSprinting(packet.isSprint());
            player.getInfo().setSneaking(packet.isShift());
            player.getInfo().setPlayerInput(PlayerInput.getFromPacket(packet));

            player.getBukkitPlayer().sendMessage("packet: " + packet.isForward());
        } else if(event.getPacketType().equals(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION)
                || event.getPacketType().equals(PacketType.Play.Client.PLAYER_POSITION)
                || event.getPacketType().equals(PacketType.Play.Client.PLAYER_ROTATION)
                || event.getPacketType().equals(PacketType.Play.Client.PLAYER_FLYING)) {
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            wrapped = packet;
            if (player.getMovement().isExcuseNextFlying()) {
                player.getMovement().setExcuseNextFlying(false);
                return false;
            }

            if (timestamp - player.getLagInfo().getLastFlying() <= 15) {
                player.getLagInfo().getLastPacketDrop().reset();
            }

            player.getLagInfo().setLastFlying(timestamp);

            player.getEntityLocationHandler().onFlying();

            if (player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_17)
                    && packet.hasPositionChanged() && packet.hasRotationChanged()
                    && MovementUtils.isSameLocation(new KLocation(
                            packet.getLocation().getX(),
                            packet.getLocation().getY(),
                            packet.getLocation().getZ()),
                    player.getMovement().getTo().getLoc())) {
                player.getMovement().setExcuseNextFlying(true);
            }

            player.getMovement().process(packet);

            var result = player.getCheckHandler().callSyncPacket(wrapped, timestamp);
            player.getVelocityHandler().onFlyingPost(packet);

            return result;
        } else if(event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

            wrapped = packet;

            // Check for isUnmount()
            if (player.getBukkitPlayer().isInsideVehicle() && packet.isUnmount()) {
                player.getInfo().getVehicleSwitch().reset();
                player.getInfo().setInVehicle(false);
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.ENTITY_ACTION)) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            wrapped = packet;

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
                case START_FLYING_WITH_ELYTRA: {
                    if(player.isGlidePossible()) {
                        player.getInfo().setGliding(true);
                    }
                    break;
                }
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            wrapped = packet;

            FakeMob mob = Anticheat.INSTANCE.getFakeTracker().getEntityById(packet.getEntityId());

            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
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
                    Optional<Entity> target = Anticheat.INSTANCE.getWorldInfo(player.getBukkitPlayer().getWorld()).getEntity(packet.getEntityId());

                    if(target.isPresent() && target.get() instanceof LivingEntity entity) {
                        if (player.getInfo().lastFakeBotHit.isPassed(400) && Math.random() > 0.9) {
                            player.getEntityLocationHandler().canCreateMob.add(entity.getEntityId());
                        }
                        player.getInfo().setTarget(entity);
                    }
                }
                player.getInfo().lastAttack.reset();
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.ANIMATION)) {
            WrapperPlayClientAnimation packet = new WrapperPlayClientAnimation(event);

            wrapped = packet;

            if(packet.getHand() == InteractionHand.MAIN_HAND) {
                long delta = timestamp - player.getInfo().lastArmSwing;

                player.getInfo().cps.add(1000D / delta, timestamp);
                player.getInfo().lastArmSwing = timestamp;
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);

            wrapped = packet;
            Vector3i pos = packet.getBlockPosition();
            Optional<ItemStack> stack = packet.getItemStack();

            player.getInfo().getLastBlockPlace().reset();

            // Used item
            if (pos.getX() == -1 && (pos.getY() == 255 | pos.getY() == -1) && pos.getZ() == -1
                    && stack.isPresent()
                    && BlockUtils.isUsable(SpigotConversionUtil.toBukkitItemMaterial(stack.get().getType()))) {
                player.getInfo().getLastUseItem().reset();
            }

            player.getBlockUpdateHandler().onPlace(packet);
        } else if(event.getPacketType().equals(PacketType.Play.Client.PLAYER_DIGGING)) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            wrapped = packet;

            player.getInfo().getLastBlockDig().reset();
            player.getBlockUpdateHandler().onDig(packet);
        } else if(event.getPacketType().equals(PacketType.Play.Client.CLIENT_STATUS)) {
            WrapperPlayClientClientStatus packet = new WrapperPlayClientClientStatus(event);

            wrapped = packet;

            if (packet.getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                player.getInfo().setInventoryOpen(true);
                player.getInfo().lastInventoryOpen.reset();
                return false;
            }
        } else if(event.getPacketType().equals(PacketType.Play.Client.CLOSE_WINDOW)) {
            wrapped = new WrapperPlayClientCloseWindow(event);
            player.getInfo().setInventoryOpen(false);
        } else if(event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);

            wrapped = packet;
            if (packet.getChannelName().equals("Time|Receive")) {
                ByteArrayDataInput serial = ByteStreams.newDataInput(packet.getData());

                long serverTime = serial.readLong();
                long clientReceivedTime = serial.readLong();

                long serverPing = clientReceivedTime - serverTime;
                long clientToServer = timestamp - clientReceivedTime;
                long totalFeedback = timestamp - serverTime;

                player.getBukkitPlayer().sendMessage(String.format("total: %sms client-server: %sms server-client: %sms", totalFeedback, clientToServer, serverPing));
            }
        } else {
            wrapped = new PacketWrapper<>(event);
        }

        if(player.sniffing) {
            player.sniffedPackets.add("[" + Anticheat.INSTANCE.getKeepaliveProcessor().tick + "] " + event.getPacketType().getName()
                    + ": " + wrapped);
        }

        return player.getCheckHandler().callSyncPacket(wrapped, timestamp);
    }

    public boolean processSend(APlayer player, PacketSendEvent event) {
        long timestamp = System.currentTimeMillis();

        Object wrapped;

        if(event.getPacketType().equals(PacketType.Play.Server.PLAYER_ABILITIES)) {
            WrapperPlayServerPlayerAbilities packet = new WrapperPlayServerPlayerAbilities(event);

            wrapped = packet;

            player.getInfo().getLastAbilities().reset();


            player.runInstantAction(ia -> {
                if (!ia.isEnd()) {
                    player.getInfo().getPossibleCapabilities().add(new PlayerCapabilities(packet));
                } else if (player.getInfo().getPossibleCapabilities().size() > 1) {
                    player.getInfo().getPossibleCapabilities().clear();
                }
            }, true);
        } else if(event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);

            wrapped = packet;

            player.getBlockUpdateHandler().runUpdate(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);

            wrapped = packet;

            player.getBlockUpdateHandler().runUpdate(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);

            wrapped = packet;

            player.getBlockUpdateHandler().runUpdate(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.MAP_CHUNK_BULK) {
            WrapperPlayServerChunkDataBulk packet = new WrapperPlayServerChunkDataBulk(event);

            wrapped = packet;

            player.getBlockUpdateHandler().runUpdate(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(event);

            wrapped = packet;
            player.getPotionHandler().onPotionEffect(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.EXPLOSION) {
            WrapperPlayServerExplosion packet = new WrapperPlayServerExplosion(event);

            wrapped = packet;
            Vector3d velocity = packet.getKnockback();

            player.getInfo().getVelocityHistory().add(velocity);
            player.getInfo().setDoingVelocity(true);

            player.runInstantAction(ia -> {
                if(!ia.isEnd()) {
                    player.getVelocityHandler().onPre(packet);
                } else {
                    if (player.getInfo().getVelocityHistory().contains(velocity))
                        player.getOnVelocityTasks().forEach(task -> task.accept(velocity));

                    player.getVelocityHandler().onPost(packet);
                    if (player.getInfo().getVelocityHistory().contains(velocity)) {
                        player.getInfo().setDoingVelocity(false);
                        player.getInfo().getVelocity().reset();
                        synchronized (player.getInfo().getVelocityHistory()) {
                            player.getInfo().getVelocityHistory().remove(velocity);
                        }
                    }
                }
            }, true);
            player.runKeepaliveAction(ka ->  player.getVelocityHandler().onPost(packet), 1);
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);

            wrapped = packet;

            if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) {
                Vector3d velocity = packet.getVelocity();
                player.getInfo().getVelocityHistory().add(velocity);
                player.getInfo().setDoingVelocity(true);
                player.runInstantAction(ia -> {
                    if (!ia.isEnd()) {
                        player.getVelocityHandler().onPre(packet);
                    } else {
                        player.getInfo().setDoingVelocity(false);
                        player.getVelocityHandler().onPost(packet);
                        if (player.getInfo().getVelocityHistory().contains(velocity))
                            player.getOnVelocityTasks().forEach(task -> task.accept(velocity));
                        if (player.getInfo().getVelocityHistory().contains(velocity)) {
                            player.getInfo().getVelocity().reset();
                            synchronized (player.getInfo().getVelocityHistory()) {
                                player.getInfo().getVelocityHistory().remove(velocity);
                            }
                        }
                    }
                }, true);
            }
        } else if(event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            wrapped = new WrapperPlayServerRespawn(event);
            if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_14)) {
                player.runKeepaliveAction(k -> player.getBukkitPlayer().setSprinting(false), 1);
            }
            player.runKeepaliveAction(ka -> player.getInfo().lastRespawn.reset());
            player.runKeepaliveAction(ka -> player.getBlockUpdateHandler()
                    .setMinHeight(player.getDimensionType()));
        } else if(event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            WrapperPlayServerPlayerPositionAndLook packet = new WrapperPlayServerPlayerPositionAndLook(event);

            wrapped = packet;

            player.runKeepaliveAction(ka ->
                    player.getBlockUpdateHandler().setMinHeight(player.getDimensionType()));
            player.getMovement().addPosition(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            WrapperPlayServerAttachEntity packet = new WrapperPlayServerAttachEntity(event);

            wrapped = packet;
            if(packet.getHoldingId() != -1) {
                player.getInfo().setInVehicle(true);
                player.getInfo().getVehicleSwitch().reset();
            } else {
                player.getInfo().setInVehicle(false);
                player.getInfo().getVehicleSwitch().reset();
            }
        } else if(event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);

            wrapped = packet;

            player.getEntityLocationHandler().onEntityDestroy(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);

            wrapped = packet;

            player.getEntityLocationHandler().onTeleportSent(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_MOVEMENT) {
            WrapperPlayServerEntityMovement packet = new WrapperPlayServerEntityMovement(event);

            wrapped = packet;

            player.getEntityLocationHandler().onRelPosition(new WPacketPlayOutEntity(packet));
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(event);

            wrapped = packet;

            player.getEntityLocationHandler().onRelPosition(new WPacketPlayOutEntity(packet));
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION) {
            WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(event);

            wrapped = packet;

            player.getEntityLocationHandler().onRelPosition(new WPacketPlayOutEntity(packet));
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);

            wrapped = packet;

            player.getEntityLocationHandler().onRelPosition(new WPacketPlayOutEntity(packet));
        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            wrapped = packet;

            double x = packet.getPosition().getX();
            double y = packet.getPosition().getY();
            double z = packet.getPosition().getZ();

            player.getEntityLocationHandler().getTrackedEntities().put(packet.getEntityId(),
                    new TrackedEntity(packet.getEntityId(), packet.getEntityType(),
                            new KLocation(x, y, z, packet.getYaw(), packet.getPitch())));
        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);

            wrapped = packet;

            double x = packet.getPosition().getX();
            double y = packet.getPosition().getY();
            double z = packet.getPosition().getZ();

            player.getEntityLocationHandler().getTrackedEntities().put(packet.getEntityId(),
                    new TrackedEntity(packet.getEntityId(), packet.getEntityType(),
                            new KLocation(x, y, z, packet.getYaw(), packet.getPitch())));
        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packet = new WrapperPlayServerSpawnPlayer(event);

            wrapped = packet;

            double x = packet.getPosition().getX();
            double y = packet.getPosition().getY();
            double z = packet.getPosition().getZ();


            player.getEntityLocationHandler().getTrackedEntities().put(packet.getEntityId(),
                    new TrackedEntity(packet.getEntityId(), EntityTypes.PLAYER,
                            new KLocation(x, y, z, packet.getYaw(),packet.getPitch())));
        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_PAINTING) {
            WrapperPlayServerSpawnPainting packet = new WrapperPlayServerSpawnPainting(event);

            wrapped = packet;

            int x = packet.getPosition().getX();
            int y = packet.getPosition().getY();
            int z = packet.getPosition().getZ();

            player.getEntityLocationHandler().getTrackedEntities().put(packet.getEntityId(),
                    new TrackedEntity(packet.getEntityId(), EntityTypes.PAINTING, new KLocation(x, y, z)));
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            WrapperPlayServerEntityPositionSync packet = new WrapperPlayServerEntityPositionSync(event);

            wrapped = packet;

            player.getEntityLocationHandler().onPositionSync(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            wrapped = new WrapperPlayServerEntityMetadata(event);
        }else if(event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);

            wrapped = packet;

            player.getEntityLocationHandler().onEntityDestroy(packet);
        } else if(event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            wrapped = new WrapperPlayServerCloseWindow(event);
            player.runKeepaliveAction(ka -> player.getInfo().setInventoryOpen(false));
        } else if(event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            wrapped = new WrapperPlayServerOpenWindow(event);
            player.runKeepaliveAction(ka -> {
                player.getInfo().setInventoryOpen(true);
                player.getInfo().lastInventoryOpen.reset();
            });
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
            wrapped = packet;

            if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) {
                var serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
                ripTide: {
                    if(serverVersion.isNewerThanOrEquals(ServerVersion.V_1_13)
                            && player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                        var entityMetadata = packet.getEntityMetadata().stream()
                                .filter(entityData -> entityData.getIndex() ==
                                        (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_17) ? 8 : 7))
                                .findFirst();

                        if(entityMetadata.isEmpty() || !(entityMetadata.get().getValue() instanceof Byte value)) {
                            break ripTide;
                        }

                        boolean isRiptiding = (value & 0x04) == 0x04;

                        player.runKeepaliveAction(ka -> player.getInfo().setRiptiding(isRiptiding));
                    }
                }

                action: {
                    var entityMetadata = packet.getEntityMetadata().stream()
                            .filter(entityData -> entityData.getIndex() == 0)
                            .findFirst();

                    if(entityMetadata.isEmpty() || !(entityMetadata.get().getValue() instanceof Byte value)) {
                        break action;
                    }

                    boolean isGliding = (value & 0x80) == 0x80
                            && player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
                    boolean isSwimming = (value & 0x10) == 0x10;
                    boolean isSprinting = (value & 0x8) == 0x8;

                    player.runKeepaliveAction(ka -> {
                        player.getInfo().setSwimming(isSwimming);
                        player.getInfo().setGliding(isGliding);
                        player.getInfo().setSprinting(isSprinting);
                    });
                }
            }

        } else {
            wrapped = new PacketWrapper<>(event);
        }

        if(player.sniffing) {
            player.sniffedPackets.add("[" + Anticheat.INSTANCE.getKeepaliveProcessor().tick + "] " + event.getPacketType().getName()
                    + ": " + wrapped);
        }

        return player.getCheckHandler().callSyncPacket(wrapped, timestamp);
    }
}
