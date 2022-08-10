package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntity;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityTeleport;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EntityLocationHandler {

    private final APlayer data;

    private final Map<UUID, EntityLocation> entityLocationMap = new ConcurrentHashMap<>();
    private final Timer lastFlying = new MillisTimer();
    public int streak;

    private static final EnumSet<EntityType> allowedEntityTypes = EnumSet.of(EntityType.ZOMBIE, EntityType.SHEEP,
            EntityType.BLAZE, EntityType.SKELETON, EntityType.PLAYER, EntityType.VILLAGER, EntityType.IRON_GOLEM,
            EntityType.WITCH, EntityType.COW, EntityType.CREEPER);

    /**
     *
     * Returns the EntityLocation based on the provided Entity's UUID. May be null if the Entity is not
     * being tracked, so we use an Optional since it could be non existent.
     *
     * @param entity Entity
     * @return Optional<EntityLocation></EntityLocation>
     */
    public Optional<EntityLocation> getEntityLocation(Entity entity) {
        return Optional.ofNullable(entityLocationMap.get(entity.getUniqueId()));
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

        entityLocationMap.values().forEach(EntityLocation::interpolateLocation);
        lastFlying.reset();
    }

    /**
     *
     * Processing PacketPlayOutRelativePosition for updating entity locations in a relative manner.
     *
     * @param packet WrappedOutRelativePosition
     */
    void onRelPosition(WPacketPlayOutEntity packet) {
        Optional<Entity> op = Anticheat.INSTANCE.getWorldInfo(data.getBukkitPlayer().getWorld()).getEntity(packet.getId());

        if(!op.isPresent()) return;

        Entity entity = op.get();

        if(!allowedEntityTypes.contains(entity.getType())) return;

        EntityLocation eloc = entityLocationMap.computeIfAbsent(entity.getUniqueId(),
                key -> new EntityLocation(entity));

        runAction(entity, () -> {
            //We don't need to do version checking here. Atlas handles this for us.
            eloc.newX += (byte)packet.getX();
            eloc.newY += packet.getY();
            eloc.newZ += packet.getZ();
            eloc.newYaw += packet.getYaw();
            eloc.newPitch += packet.getPitch();

            eloc.increment = 3;

            eloc.interpolatedLocations.clear();
        });
    }

    /**
     *
     * Processing PacketPlayOutEntityTeleport to update locations in a non-relative manner.
     *
     * @param packet WrappedOutEntityTeleportPacket
     */
    void onTeleportSent(WPacketPlayOutEntityTeleport packet) {
        Optional<Entity> op = Anticheat.INSTANCE.getWorldInfo(data.getBukkitPlayer().getWorld()).getEntity(packet.getEntityId());

        if(!op.isPresent()) return;

        Entity entity = op.get();

        if(!allowedEntityTypes.contains(entity.getType())) return;

        EntityLocation eloc = entityLocationMap.computeIfAbsent(entity.getUniqueId(),
                key -> new EntityLocation(entity));

        runAction(entity, () -> {
            if(data.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                if (!(Math.abs(eloc.x - packet.getX()) >= 0.03125D)
                        && !(Math.abs(eloc.y - packet.getY()) >= 0.015625D)
                        && !(Math.abs(eloc.z - packet.getZ()) >= 0.03125D)) {
                    eloc.increment = 0;
                    //We don't need to do version checking here. Atlas handles this for us.
                    eloc.newX = eloc.x = packet.getX();
                    eloc.newY = eloc.y = packet.getY();
                    eloc.newZ = eloc.z = packet.getZ();
                    eloc.newYaw = eloc.yaw = packet.getYaw();
                    eloc.newPitch = eloc.pitch = packet.getPitch();
                    eloc.interpolatedLocations.clear();
                } else {
                    eloc.newX = packet.getX();
                    eloc.newY = packet.getY();
                    eloc.newZ = packet.getZ();
                    eloc.newYaw = packet.getYaw();
                    eloc.newPitch = packet.getPitch();

                    eloc.increment = 3;
                    eloc.interpolatedLocations.clear();
                }
            } else {
                //We don't need to do version checking here. Atlas handles this for us.
                eloc.newX = packet.getX();
                eloc.newY = packet.getY();
                eloc.newZ = packet.getZ();
                eloc.newYaw = packet.getYaw();
                eloc.newPitch = packet.getPitch();

                eloc.increment = 3;
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
    private void runAction(Entity entity, Runnable action) {
        if(data.getInfo().getTarget() != null && data.getInfo().getTarget().getEntityId() == entity.getEntityId()) {
            data.runInstantAction(ia -> {
                if(!ia.isEnd()) {
                    action.run();
                } else entityLocationMap.get(entity.getUniqueId()).oldLocations.clear();
            });
        } else {
            data.runKeepaliveAction(keepalive -> action.run());
            data.runKeepaliveAction(keepalive ->
                    entityLocationMap.get(entity.getUniqueId()).oldLocations.clear(), 1);
        }
    }
}
