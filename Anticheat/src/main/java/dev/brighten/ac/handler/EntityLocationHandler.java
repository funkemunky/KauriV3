package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.packet.WPacketPlayOutEntity;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.PacketEventsUtil;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.world.types.RayCollision;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class EntityLocationHandler {

    private final APlayer data;

    private final Map<UUID, Tuple<EntityLocation, EntityLocation>> entityLocationMap = new ConcurrentHashMap<>();
    private final Map<Integer, List<FakeMob>> fakeMobs = new Int2ObjectArrayMap<>();
    private final Map<Integer, Integer> fakeMobToEntityId = new Int2ObjectArrayMap<>();
    private final Timer lastFlying = new MillisTimer();

    public Set<Integer> canCreateMob = new HashSet<>();
    public int streak;
    public AtomicBoolean clientHasEntity = new AtomicBoolean(false);

    private final Set<EntityType> allowedEntityTypes = Set.of(EntityTypes.ZOMBIE, EntityTypes.SHEEP,
            EntityTypes.BLAZE, EntityTypes.SKELETON, EntityTypes.PLAYER, EntityTypes.VILLAGER, EntityTypes.IRON_GOLEM,
            EntityTypes.WITCH, EntityTypes.COW, EntityTypes.CREEPER);

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

    public Optional<List<FakeMob>> getFakeMob(int entityId) {
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

        processZombie();

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
        Optional<Entity> op = Anticheat.INSTANCE.getWorldInfo(data.getBukkitPlayer().getWorld())
                .getEntity(packet.getId());

        if(op.isEmpty()) return;

        Entity entity = op.get();

        var type = SpigotConversionUtil.fromBukkitEntityType(entity.getType());

        if(type == null || !allowedEntityTypes.contains(type)) return;

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
    void onTeleportSent(WrapperPlayServerEntityTeleport packet) {
        Optional<Entity> op = Anticheat.INSTANCE.getWorldInfo(data.getBukkitPlayer().getWorld())
                .getEntity(packet.getEntityId());

        if(op.isEmpty()) return;

        Entity entity = op.get();

        EntityType type = PacketEventsUtil.convertBukkitEntityType(entity.getType());

        if(type == null || !allowedEntityTypes.contains(type)) return;

        val tuple = entityLocationMap.computeIfAbsent(entity.getUniqueId(),
                key -> {
                    createFakeMob(packet.getEntityId(), entity.getLocation());
                    return new Tuple<>(new EntityLocation(entity), null);
                });

        processFakeMobs(packet.getEntityId(), false, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());

        EntityLocation eloc = tuple.one;

        tuple.two = tuple.one.clone();

        runAction(entity, () -> {
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
            }, true);
        } else {
            data.runKeepaliveAction(keepalive -> action.run());
            data.runKeepaliveAction(keepalive ->
                    entityLocationMap.get(entity.getUniqueId()).two = null, 1);
        }
    }

    public void onEntityDestroy(WrapperPlayServerDestroyEntities packet) {
        for(int id : packet.getEntityIds()) {
            if(fakeMobs.containsKey(id)) {
                List<FakeMob> mobs = fakeMobs.get(id);

                for (FakeMob mob : mobs) {
                    mob.despawn();
                    fakeMobToEntityId.remove(mob.getEntityId());
                }
                fakeMobs.remove(id);
                clientHasEntity.set(false);
            }
        }
    }

    public void removeFakeMob(int id) {
        if(fakeMobs.containsKey(id)) {
            List<FakeMob> mobs = fakeMobs.get(id);

            for (FakeMob mob : mobs) {
                mob.despawn();
                fakeMobToEntityId.remove(mob.getEntityId());
            }
            fakeMobs.remove(id);
        }
        clientHasEntity.set(false);
    }

    private static final double[] offsets = new double[]{-1.25, 0, 1.25};

    private void createFakeMob(int entityId, Location location) {
        if(!canCreateMob.contains(entityId)) return;

        List<FakeMob> mobs = new ArrayList<>();

        clientHasEntity.set(false);
        for (double offset : offsets) {
            FakeMob mob = new FakeMob(EntityTypes.MAGMA_CUBE);

            List<EntityData<?>> types = new ArrayList<>();
            EntityData<?> entityData = new EntityData<>(7, EntityDataTypes.INT, 10);
            types.add(entityData);

            // Setting Magma cube size to size 10
            mob.spawn(true, location.clone().add(offset, offset, offset),
                    types, data);

            fakeMobToEntityId.put(mob.getEntityId(), entityId);

            mobs.add(mob);
        }

        KLocation eyeLoc = data.getMovement().getTo().getLoc().clone()
                .add(0, data.getEyeHeight() / 2.5, 0);

        RayCollision collision = new RayCollision(eyeLoc.toVector(), eyeLoc.getDirection());

        Vector point = collision.collisionPoint(0.4);

        FakeMob mob = new FakeMob(EntityTypes.SLIME);
        List<EntityData<?>> types = new ArrayList<>();
        EntityData<?> entityData = new EntityData<>(7, EntityDataTypes.INT, 5);

        types.add(entityData);
        mob.spawn(true, point.toLocation(location.getWorld()), types, data);

        fakeMobToEntityId.put(mob.getEntityId(), data.getBukkitPlayer().getEntityId());


        this.fakeMobs.put(data.getBukkitPlayer().getEntityId(), new ArrayList<>(Collections.singletonList(mob)));
        this.fakeMobs.put(entityId, mobs);
        canCreateMob.remove(entityId);

        data.runKeepaliveAction(ka -> clientHasEntity.set(true));
    }

    public void processZombie() {
        List<FakeMob> fakeMobs = this.fakeMobs.get(data.getBukkitPlayer().getEntityId());

        if(fakeMobs == null) return;

        if(fakeMobs.size() > 1) {
            fakeMobs.forEach(fakeMob -> removeFakeMob(fakeMob.getEntityId()));
        }

        for (FakeMob fakeMob : fakeMobs) {
            if(fakeMob.getType() == EntityTypes.SLIME) {
                KLocation eyeLoc = data.getMovement().getTo().getLoc().clone().add(0, 0.6, 0);

                RayCollision collision = new RayCollision(eyeLoc.toVector(), eyeLoc.getDirection());

                Vector point = collision.collisionPoint(0.4);

                fakeMob.teleport(point.getX(), point.getY(), point.getZ(), 0 ,0);
                break;
            }
        }
    }

    public void processFakeMobs(int entityId, boolean rel, double x, double y, double z) {
        List<FakeMob> fakeMobs = this.fakeMobs.get(entityId);

        if(fakeMobs == null) {
            if(!rel) {
                createFakeMob(entityId, new Location(data.getBukkitPlayer().getWorld(), x, y, z));
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
