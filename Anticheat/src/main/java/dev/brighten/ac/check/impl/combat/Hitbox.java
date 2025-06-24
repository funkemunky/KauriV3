package dev.brighten.ac.check.impl.combat;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@CheckData(name = "Hitbox", checkId = "hitboxa", type = CheckType.COMBAT)
public class Hitbox extends Check {
    private float buffer;
    private int hbuffer;

    public Timer lastAimOnTarget = new TickTimer(), lastPosition = new TickTimer();
    private final Queue<Tuple<TrackedEntity, KLocation>> attacks = new LinkedBlockingQueue<>();

    private final Set<EntityType> allowedEntityTypes = Set.of(EntityTypes.ZOMBIE, EntityTypes.SHEEP,
            EntityTypes.BLAZE, EntityTypes.SKELETON, EntityTypes.PLAYER, EntityTypes.VILLAGER, EntityTypes.IRON_GOLEM,
            EntityTypes.WITCH, EntityTypes.COW, EntityTypes.CREEPER);

    public Hitbox(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientInteractEntity> useEntity = packet -> {

        if(packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK)
            return;

        Optional<TrackedEntity> entity = player.getEntityLocationHandler().getTrackedEntity(packet.getEntityId());
        if(entity.isEmpty() || !allowedEntityTypes.contains(entity.get().getEntityType())) return;

        attacks.add(new Tuple<>(entity.get(),
                player.getMovement().getTo().getLoc().clone()));
    };

    //TODO Figure out how to make the check more sensitive without compromising network stability
    //Aka figure out how to minimize the amount of previous locations needed to process to keep network
    //stability. like shortening the amount stored, or removing older ones.
    @Bind
    WAction<WrapperPlayClientPlayerFlying> onFlying = packet -> {
        if(player.getInfo().isCreative() || player.getInfo().isInVehicle()) {
            attacks.clear();
            return;
        }
        Tuple<TrackedEntity, KLocation> target;

        while((target = attacks.poll()) != null) {
            //Updating new entity loc

            TrackedEntity entity = target.one;

            final KLocation to = target.two;

            if(entity.getNewEntityLocation().x == 0 && entity.getNewEntityLocation().y == 0 & entity.getNewEntityLocation().z == 0) {
                return;
            }

            double distance = Double.MAX_VALUE;
            boolean collided = false; //Using this to compare smaller numbers than Double.MAX_VALUE. Slightly faster

            List<SimpleCollisionBox> boxes = new ArrayList<>();
            double expand = 0.005;
            if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_9)) {
                expand+= 0.1;
            }

            //Accounting for potential movement updates not sent to the server
            if(lastPosition.isPassed(1)) {
                expand+= 0.03;
            }

            if(entity.getOldEntityLocation() != null) {
                for (KLocation oldLocation : entity.getNewEntityLocation().interpolatedLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toLocation(player.getBukkitPlayer().getWorld()), target.one);

                    boxes.add(box.expand(expand));
                }
                for (KLocation oldLocation : entity.getOldEntityLocation().interpolatedLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toLocation(player.getBukkitPlayer().getWorld()), target.one);

                    boxes.add(box.expand(expand));
                }
            } else {
                for (KLocation oldLocation : entity.getNewEntityLocation().interpolatedLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toLocation(player.getBukkitPlayer().getWorld()), target.one);

                    boxes.add(box.expand(expand));
                }
            }

            if(boxes.isEmpty()) return;

            int hits = 0;

            boolean didSneakOrElytra = player.getInfo().getLastSneak().isNotPassed(40)
                    || player.getInfo().getLastElytra().isNotPassed(40);

            List<Vector> directions = new ArrayList<>(Arrays.asList(MathUtils.getDirection(
                            player.getMovement().getTo().getLoc().getYaw(),
                            player.getMovement().getTo().getLoc().getPitch()),
                    MathUtils.getDirection(player.getMovement().getFrom().getLoc().getYaw(),
                            player.getMovement().getTo().getLoc().getPitch())));

            if(!didSneakOrElytra) {
                to.add(0, 1.62f, 0);
                for (Vector direction : directions) {
                    for (SimpleCollisionBox targetBox : boxes) {
                        final AxisAlignedBB vanillaBox = new AxisAlignedBB(targetBox);

                        Vec3D intersectTo = vanillaBox.rayTrace(to.toVector(), direction, 10);

                        if(intersectTo != null) {
                            lastAimOnTarget.reset();
                            hits++;
                            distance = Math.min(distance, intersectTo
                                    .distanceSquared(new Vec3D(to.getX(), to.getY(), to.getZ())));
                            collided = true;
                        }
                    }
                }
                //Checking all possible eyeheights since client actions notoriously desync from the server side
            } else {
                for (Vector direction : directions) {
                    for (double eyeHeight : player.getMovement().getEyeHeights()) {
                        for (SimpleCollisionBox targetBox : boxes) {
                            final AxisAlignedBB vanillaBox = new AxisAlignedBB(targetBox);

                            KLocation from = to.clone();

                            from.add(0, eyeHeight, 0);
                            Vec3D intersectTo = vanillaBox.rayTrace(from.toVector(), direction, 10);

                            if(intersectTo != null) {
                                lastAimOnTarget.reset();
                                hits++;
                                distance = Math.min(distance, intersectTo
                                        .distanceSquared(new Vec3D(from.getX(), from.getY(), from.getZ())));
                                collided = true;
                            }
                        }
                    }
                }
            }

            if(collided) {
                hbuffer = 0;
                distance = Math.sqrt(distance);
                if(distance > 3.001) {
                    if(++buffer > 1) {
                        flag("d=%.3f>-3.0", distance);
                        buffer = Math.min(1, buffer);
                    }
                    Anticheat.INSTANCE.getRunUtils().taskAsync(() -> boxes.forEach(box -> box.draw(ParticleTypes.FLAME, player)));
                } else if(buffer > 0) buffer-= 0.02f;

                if(hbuffer > 0) hbuffer--;

                debug("buffer: %.3f distance=%.2f hits=%s sneaking=%s", buffer, distance, hits,
                        player.getInfo().isSneaking());
            } else if(player.getEntityLocationHandler().streak > 1) {
                if (++hbuffer > 5) {
                    flag("%.1f;%.1f;%.1f", entity.getNewEntityLocation().x, entity.getNewEntityLocation().y, entity.getNewEntityLocation().z);
                }
                debug("Missed!");
            }
        }
        if(packet.hasPositionChanged())
            lastPosition.reset();
    };

}
