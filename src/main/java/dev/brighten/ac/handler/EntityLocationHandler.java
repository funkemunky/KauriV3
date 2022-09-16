package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.out.*;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EntityLocationHandler {

    private final APlayer data;

    private final Map<UUID, Tuple<EntityLocation, EntityLocation>> entityLocationMap = new ConcurrentHashMap<>();
    private final Map<Integer, FakeMob> fakeMobs = new Int2ObjectArrayMap<>();
    private final Map<Integer, Integer> fakeMobToEntityId = new Int2ObjectArrayMap<>();
    private final Timer lastFlying = new MillisTimer();

    public Set<Integer> canCreateMob = new HashSet<>();
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
    public Optional<Tuple<EntityLocation, EntityLocation>> getEntityLocation(Entity entity) {
        return Optional.ofNullable(entityLocationMap.get(entity.getUniqueId()));
    }

    public Optional<FakeMob> getFakeMob(int entityId) {
        return Optional.ofNullable(fakeMobs.get(entityId));
    }

    public Optional<Integer> getTargetOfFakeMob(int fakeMobId) {
        return Optional.ofNullable(fakeMobToEntityId.get(fakeMobId));
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

        entityLocationMap.values().forEach(eloc -> {
            if(eloc.one != null) {
                if(eloc.one.interpolatedLocations.size() > 1 && eloc.one.increment == 0) {
                    eloc.one.interpolatedLocations.removeFirst();
                }
                eloc.one.interpolateLocation();
            }
            if(eloc.two != null) {
                if(eloc.two.interpolatedLocations.size() > 1 && eloc.two.increment == 0) {
                    eloc.two.interpolatedLocations.removeFirst();
                }
                eloc.two.interpolateLocation();
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
        Optional<Entity> op = Anticheat.INSTANCE.getWorldInfo(data.getBukkitPlayer().getWorld()).getEntity(packet.getId());

        if(!op.isPresent()) return;

        Entity entity = op.get();

        if(!allowedEntityTypes.contains(entity.getType())) return;

        val tuple = entityLocationMap.computeIfAbsent(entity.getUniqueId(),
                key -> {
                     createFakeMob(packet.getId(), entity.getLocation());
                     return new Tuple<>(new EntityLocation(entity), null);
                });

        processFakeMobs(packet.getId(), true, packet.getX(), packet.getY(), packet.getZ());

        EntityLocation eloc = tuple.one;

        tuple.two = tuple.one.clone();

        runAction(entity, () -> {
            //We don't need to do version checking here. Atlas handles this for us.
            eloc.newX += packet.getX();
            eloc.newY += packet.getY();
            eloc.newZ += packet.getZ();
            eloc.newYaw += packet.getYaw();
            eloc.newPitch += packet.getPitch();

            eloc.increment = 3;
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


        val tuple = entityLocationMap.computeIfAbsent(entity.getUniqueId(),
                key -> {
                    createFakeMob(packet.getEntityId(), entity.getLocation());
                    return new Tuple<>(new EntityLocation(entity), null);
                });

        processFakeMobs(packet.getEntityId(), false, packet.getX(), packet.getY(), packet.getZ());

        EntityLocation eloc = tuple.one;

        tuple.two = tuple.one.clone();

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
                } else {
                    eloc.newX = packet.getX();
                    eloc.newY = packet.getY();
                    eloc.newZ = packet.getZ();
                    eloc.newYaw = packet.getYaw();
                    eloc.newPitch = packet.getPitch();

                    eloc.increment = 3;
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
                } else entityLocationMap.get(entity.getUniqueId()).two = null;
            });
        } else {
            data.runKeepaliveAction(keepalive -> action.run());
            data.runKeepaliveAction(keepalive ->
                    entityLocationMap.get(entity.getUniqueId()).two = null, 1);
        }
    }

    public void onSpawnEntity(WPacketPlayOutNamedEntitySpawn packet) {
        createFakeMob(packet.getEntityId(), new Location(data.getBukkitPlayer().getWorld(), packet.getX(), packet.getY(), packet.getZ()));
    }

    public void onSpawnEntity(WPacketPlayOutSpawnEntityLiving packet) {
        if(!allowedEntityTypes.contains(packet.getType())) return;

        createFakeMob(packet.getEntityId(), new Location(data.getBukkitPlayer().getWorld(), packet.getX(), packet.getY(), packet.getZ()));
    }

    public void onEntityDestroy(WPacketPlayOutEntityDestroy packet) {
        for(int id : packet.getEntityIds()) {
            if(fakeMobs.containsKey(id)) {
                FakeMob mob = fakeMobs.get(id);
                mob.despawn();
                fakeMobToEntityId.remove(mob.getEntityId());
                fakeMobs.remove(id);
            }
        }
    }

    public void removeFakeMob(int id) {
        if(fakeMobs.containsKey(id)) {
            FakeMob mob = fakeMobs.get(id);
            mob.despawn();
            fakeMobToEntityId.remove(mob.getEntityId());
            fakeMobs.remove(id);
        }
    }

    private void createFakeMob(int entityId, Location location) {
        if(!canCreateMob.contains(entityId)) return;
        FakeMob mob = new FakeMob(EntityType.GIANT);

        mob.spawn(true, location, data);

        this.fakeMobs.put(entityId, mob);
        fakeMobToEntityId.put(mob.getEntityId(), entityId);
        canCreateMob.remove(entityId);
    }

    public void processFakeMobs(int entityId, boolean rel, double x, double y, double z) {
        FakeMob fakeMob = fakeMobs.get(entityId);

        if(fakeMob == null) {
            if(!rel) {
                createFakeMob(entityId, new Location(data.getBukkitPlayer().getWorld(), x, y, z));
                fakeMob = fakeMobs.get(entityId);

                if(fakeMob == null) return;
            } else return;
        }

        if(rel) {
            fakeMob.move(x, y, z);
        } else {
            fakeMob.teleport(x, y, z, 0, 0);
        }
    }
}
