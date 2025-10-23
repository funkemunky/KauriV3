package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.packet.WPacketPlayOutEntity;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.world.types.RayCollision;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class EntityTrackHandler {

    private final APlayer data;
    private final Timer lastFlying = new MillisTimer();

    public Set<Integer> canCreateMob = new HashSet<>();
    public int streak;
    public AtomicBoolean clientHasEntity = new AtomicBoolean(false);

    private final Set<EntityType> allowedEntityTypes = Set.of(EntityTypes.ZOMBIE, EntityTypes.SHEEP,
            EntityTypes.BLAZE, EntityTypes.SKELETON, EntityTypes.PLAYER, EntityTypes.VILLAGER, EntityTypes.IRON_GOLEM,
            EntityTypes.WITCH, EntityTypes.COW, EntityTypes.CREEPER);

    public Optional<Integer> getTargetOfFakeMob(int fakeMobId) {
        for (TrackedEntity value : data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntities().values()) {
            if(value.getFakeMobs().stream().anyMatch(mob -> mob.getEntityId() == fakeMobId)) {
                return Optional.of(value.getEntityId());
            }
        }
        return Optional.empty();
    }

    /**
     *
     * We are processing PacketPlayInFlying to iterate the tracked entity locations
     *
     */
    void onFlying() {
        if(lastFlying.isNotPassed(1)) streak++;
        else {
            streak = 1;
        }

        processZombie();

        data.getWorldTracker().getCurrentWorld().get().getTrackedEntities().values().forEach(entity -> {
            var oldLoc = entity.getOldEntityLocation();
            var newLoc = entity.getNewEntityLocation();

            if(oldLoc != null) {
                if(oldLoc.interpolatedLocations.size() > 1 && oldLoc.increment == 0) {
                    oldLoc.interpolatedLocations.removeFirst();
                }
                oldLoc.interpolateLocation();
            }
            if(newLoc != null) {
                if(newLoc.interpolatedLocations.size() > 1 && newLoc.increment == 0) {
                    newLoc.interpolatedLocations.removeFirst();
                }
                newLoc.interpolateLocation();
            }
        });

        lastFlying.reset();
    }

    /**
     *
     * Processing PacketPlayOutRelativePosition for updating entity locations in a relative manner.
     *
     * @param packet WrappedOutRelativePosition
     */
    void onRelPosition(WPacketPlayOutEntity packet) {
        Optional<TrackedEntity> entity = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(packet.getId());

        if(entity.isEmpty() || !allowedEntityTypes.contains(entity.get().getEntityType())) return;

        processFakeMobs(packet.getId(), true, packet.getX(), packet.getY(), packet.getZ());

        EntityLocation eloc = entity.get().getNewEntityLocation();

        entity.get().setOldEntityLocation(entity.get().getNewEntityLocation().clone());

        runAction(entity.get(), () -> {
            //We don't need to do version checking here. Atlas handles this for us.
            eloc.newX += packet.getX();
            eloc.newY += packet.getY();
            eloc.newZ += packet.getZ();
            eloc.newYaw += packet.getYaw();
            eloc.newPitch += packet.getPitch();

            eloc.increment = 3;
        });
    }

    void onPositionSync(WrapperPlayServerEntityPositionSync packet) {
        Optional<TrackedEntity> entity = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(packet.getId());

        if(entity.isEmpty() || !allowedEntityTypes.contains(entity.get().getEntityType())) return;

        processFakeMobs(packet.getId(), false, packet.getValues().getPosition().getX(),
                packet.getValues().getPosition().getY(), packet.getValues().getPosition().getZ());

        EntityLocation eloc = entity.get().getNewEntityLocation();

        entity.get().setOldEntityLocation(entity.get().getNewEntityLocation().clone());

        runAction(entity.get(), () -> {
            //We don't need to do version checking here. Atlas handles this for us.
            eloc.newX = packet.getValues().getPosition().getX();
            eloc.newY = packet.getValues().getPosition().getY();
            eloc.newZ = packet.getValues().getPosition().getZ();
            eloc.newYaw = packet.getValues().getYaw();
            eloc.newPitch = packet.getValues().getPitch();

            eloc.increment = 3;
        });
    }
    /**
     *
     * Processing PacketPlayOutEntityTeleport to update locations in a non-relative manner.
     *
     * @param packet WrappedOutEntityTeleportPacket
     */
    void onTeleportSent(WrapperPlayServerEntityTeleport packet) {
        Optional<TrackedEntity> entity = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(packet.getEntityId());

        if(entity.isEmpty() || !allowedEntityTypes.contains(entity.get().getEntityType())) return;

        processFakeMobs(packet.getEntityId(), false, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());

        EntityLocation eloc = entity.get().getNewEntityLocation();

        entity.get().setOldEntityLocation(entity.get().getNewEntityLocation().clone());

        runAction(entity.get(), () -> {
            if(data.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                if (!(Math.abs(eloc.x - packet.getPosition().getX()) >= 0.03125D)
                        && !(Math.abs(eloc.y - packet.getPosition().getY()) >= 0.015625D)
                        && !(Math.abs(eloc.z - packet.getPosition().getZ()) >= 0.03125D)) {
                    eloc.increment = 0;
                    //We don't need to do version checking here. Atlas handles this for us.
                    eloc.newX = eloc.x = packet.getPosition().getX();
                    eloc.newY = eloc.y = packet.getPosition().getY();
                    eloc.newZ = eloc.z = packet.getPosition().getZ();
                    eloc.newYaw = eloc.yaw = packet.getYaw();
                    eloc.newPitch = eloc.pitch = packet.getPitch();
                } else {
                    eloc.newX = packet.getPosition().getX();
                    eloc.newY = packet.getPosition().getY();
                    eloc.newZ = packet.getPosition().getZ();
                    eloc.newYaw = packet.getYaw();
                    eloc.newPitch = packet.getPitch();

                    eloc.increment = 3;
                }
            } else {
                //We don't need to do version checking here. Atlas handles this for us.
                eloc.newX = packet.getPosition().getX();
                eloc.newY = packet.getPosition().getY();
                eloc.newZ = packet.getPosition().getZ();
                eloc.newYaw = packet.getYaw();
                eloc.newPitch = packet.getPitch();

                eloc.increment = 3;
            }
        });
    }

    void updateAttributes(WrapperPlayServerUpdateAttributes attributes) {
        data.runKeepaliveAction(ka -> {
            TrackedEntity tracked = data.getWorldTracker().getCurrentWorld().get()
                    .getTrackedEntity(attributes.getEntityId()).orElse(null);

            if(tracked == null) return;

            for (WrapperPlayServerUpdateAttributes.Property property : attributes.getProperties()) {
                Attribute attribute = property.getAttribute();

                if(attributes.getEntityId() == data.getBukkitPlayer().getEntityId() && attribute == Attributes.MOVEMENT_SPEED) {
                    ValuedAttribute value = tracked.getAttribute(attribute);

                    value.updateAttribute(property);
                    data.getInfo().setWalkSpeed(value.getValue());
                }

                tracked.updateAttribute(property);
            }
        });
    }

    /**
     *
     * We are running an action when a transaction is received. If the Entity provided is currently a target,
     * we want to send a tranasction on this method being run and use that to more accurately get an estimate of when
     * the client receives the transaction relative to what we want in the action. If not the target, then we use our
     * transaction system which sends one transaction every tick, and then on return runs a list of Runnables which
     * may be less accurate in some situations, but uses less processing and network resources.
     *
     * @param entity Entity
     * @param action Runnable
     */
    private void runAction(TrackedEntity entity, Runnable action) {
        if(data.getInfo().getTarget() != null && data.getInfo().getTarget().getEntityId() == entity.getEntityId()) {
            data.runInstantAction(ia -> {
                if(!ia.isEnd()) {
                    action.run();
                } else entity.setOldEntityLocation(null);
            }, true);
        } else {
            data.runKeepaliveAction(keepalive -> action.run());
            data.runKeepaliveAction(keepalive ->
                    entity.setOldEntityLocation(null), 1);
        }
    }

    public void onEntityDestroy(WrapperPlayServerDestroyEntities packet) {
        data.runKeepaliveAction(ka -> {
            for(int id : packet.getEntityIds()) {
                removeFakeMob(id);
            }
        });
    }

    public void removeFakeMob(int id) {
        if(data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntities().containsKey(id)) {
            List<FakeMob> mobs = data.getWorldTracker().getCurrentWorld().get()
                    .getTrackedEntity(id).map(TrackedEntity::getFakeMobs).orElse(new ArrayList<>());

            for (FakeMob mob : mobs) {
                mob.despawn();
            }

            mobs.clear();
        }
        clientHasEntity.set(false);
    }

    private static final double[] offsets = new double[]{-1.25, 0, 1.25};

    private void createFakeMob(int entityId, KLocation location) {
        if (!canCreateMob.contains(entityId)) return;

        Optional<TrackedEntity> trackedEntity = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(entityId);

        Optional<TrackedEntity> playerEntity = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(data.getBukkitPlayer().getEntityId());

        if(trackedEntity.isEmpty() || playerEntity.isEmpty()) return;

        for (double offset : offsets) {
            FakeMob mob = new FakeMob(EntityTypes.MAGMA_CUBE);

            List<EntityData<?>> types = new ArrayList<>();
            EntityData<?> entityData = new EntityData<>(16, EntityDataTypes.BYTE, (byte)10);
            types.add(entityData);

            // Setting Magma cube size to size 10
            mob.spawn(true, location.clone().add(offset, offset, offset),
                    types, data);

            trackedEntity.get().getFakeMobs().add(mob);
        }

        KLocation eyeLoc = data.getMovement().getTo().getLoc().clone()
                .add(0, data.getEyeHeight() / 2.5, 0);

        RayCollision collision = new RayCollision(eyeLoc.toVector(), eyeLoc.getDirection());

        Vector3d point = collision.collisionPoint(0.4);

        FakeMob mob = new FakeMob(EntityTypes.SLIME);
        List<EntityData<?>> types = new ArrayList<>();
        EntityData<?> entityData = new EntityData<>(16, EntityDataTypes.BYTE, (byte)5);

        types.add(entityData);
        mob.spawn(true, new KLocation(point.getX(), point.getY(), point.getZ()), types, data);

        playerEntity.get().getFakeMobs().add(mob);

        canCreateMob.remove(entityId);

        data.runKeepaliveAction(ka -> clientHasEntity.set(true));
    }

    public void processZombie() {
        List<FakeMob> fakeMobs = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(data.getBukkitPlayer().getEntityId())
                .map(TrackedEntity::getFakeMobs)
                .orElse(new ArrayList<>());

        if(fakeMobs.size() > 1) {
            fakeMobs.forEach(fakeMob -> removeFakeMob(fakeMob.getEntityId()));
        }

        for (FakeMob fakeMob : fakeMobs) {
            if(fakeMob.getType() == EntityTypes.SLIME) {
                KLocation eyeLoc = data.getMovement().getTo().getLoc().clone().add(0, 0.6, 0);

                RayCollision collision = new RayCollision(eyeLoc.toVector(), eyeLoc.getDirection());

                Vector3d point = collision.collisionPoint(0.4);

                fakeMob.teleport(point.getX(), point.getY(), point.getZ(), 0 ,0);
                break;
            }
        }
    }

    public void processFakeMobs(int entityId, boolean rel, double x, double y, double z) {
        List<FakeMob> fakeMobs = data.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(entityId).map(TrackedEntity::getFakeMobs).orElse(new ArrayList<>());

        if(fakeMobs.isEmpty()) {
            if(!rel) {
                createFakeMob(entityId, new KLocation(x, y, z));
            }

            if(data.getInfo().getTarget() != null && data.getInfo().getTarget().getEntityId() == entityId) {
                clientHasEntity.set(false);
            }
            return;
        }

        if(fakeMobs.size() != offsets.length + 1) {
            fakeMobs.forEach(fakeMob -> removeFakeMob(fakeMob.getEntityId()));
        }

        int current = 0;
        for (FakeMob fakeMob : fakeMobs) {
            double offset = offsets[current++];

            if(rel) {
                fakeMob.move(x, y, z);
            } else {
                fakeMob.teleport(x + offset, y + offset, z + offset, 0, 0);
            }
        }
    }
}
