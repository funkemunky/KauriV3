package dev.brighten.ac.check.impl.combat;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@CheckData(name = "Hitbox", type = CheckType.COMBAT)
public class Reach extends Check {
    private float buffer;
    private int hbuffer;

    public Timer lastAimOnTarget = new TickTimer();
    private final Queue<Tuple<Entity, KLocation>> attacks = new LinkedBlockingQueue<>();

    private static final EnumSet<EntityType> allowedEntityTypes = EnumSet.of(EntityType.ZOMBIE, EntityType.SHEEP,
            EntityType.BLAZE, EntityType.SKELETON, EntityType.PLAYER, EntityType.VILLAGER, EntityType.IRON_GOLEM,
            EntityType.WITCH, EntityType.COW, EntityType.CREEPER);

    public Reach(APlayer player) {
        super(player);
    }

    @Action
    public void onUse(WPacketPlayInUseEntity packet) {
        if(packet.getAction() == WPacketPlayInUseEntity.EnumEntityUseAction.ATTACK
                && allowedEntityTypes.contains(packet.getEntity(getPlayer().getBukkitPlayer().getWorld()).getType())) {
            attacks.add(new Tuple<>(packet.getEntity(getPlayer().getBukkitPlayer().getWorld()), getPlayer().getMovement().getTo().getLoc().clone()));
        }
    }

    //TODO Figure out how to make the check more sensitive without compromising network stability
    //Aka figure out how to minimize the amount of previous locations needed to process to keep network
    //stability. like shortening the amount stored, or removing older ones.
    @Action
    public void onFlying(WPacketPlayInFlying packet) {
        if(getPlayer().getInfo().isCreative() || getPlayer().getInfo().isInVehicle()) {
            attacks.clear();
           return;
        }
        Tuple<Entity, KLocation> target;

        while((target = attacks.poll()) != null) {
            //Updating new entity loc
            Optional<EntityLocation> optionalEloc = getPlayer().getEntityLocationHandler().getEntityLocation(target.one);

            if(!optionalEloc.isPresent()) {
                return;
            }

            final EntityLocation eloc = optionalEloc.get();

            final KLocation to = target.two;

            debug("current loc: %.4f, %.4f, %.4f", eloc.x, eloc.y, eloc.z);

            if(eloc.x == 0 && eloc.y == 0 & eloc.z == 0) {
                return;
            }

            double distance = Double.MAX_VALUE;
            boolean collided = false; //Using this to compare smaller numbers than Double.MAX_VALUE. Slightly faster

            List<SimpleCollisionBox> boxes = new ArrayList<>();
            if(eloc.oldLocations.size() > 0) {
                for (KLocation oldLocation : eloc.oldLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toVector(), target.one);

                    if(getPlayer().getPlayerVersion().isBelow(ProtocolVersion.V1_9)) {
                        box = box.expand(0.1325);
                    } else box = box.expand(0.0325);
                    boxes.add(box);
                }
                for (KLocation oldLocation : eloc.interpolatedLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toVector(), target.one);

                    if(getPlayer().getPlayerVersion().isBelow(ProtocolVersion.V1_9)) {
                        box = box.expand(0.1325);
                    } else box = box.expand(0.0325);
                    boxes.add(box);
                }
            } else {
                for (KLocation oldLocation : eloc.interpolatedLocations) {
                    SimpleCollisionBox box = (SimpleCollisionBox)
                            EntityData.getEntityBox(oldLocation.toVector(), target.one);

                    if(getPlayer().getPlayerVersion().isBelow(ProtocolVersion.V1_9)) {
                        box = box.expand(0.1325);
                    } else box = box.expand(0.0325);
                    boxes.add(box);
                }
            }

            if(boxes.size() == 0) return;

            int hits = 0;

            boolean didSneakOrElytra = getPlayer().getInfo().getLastElytra().isNotPassed(40)
                    || getPlayer().getInfo().getLastElytra().isNotPassed(40);

            List<Vector> directions = new ArrayList<>(Arrays.asList(MathUtils.getDirection(
                    getPlayer().getMovement().getTo().getLoc().yaw,
                    getPlayer().getMovement().getTo().getLoc().pitch),
                    MathUtils.getDirection(getPlayer().getMovement().getFrom().getLoc().yaw,
                            getPlayer().getMovement().getTo().getLoc().pitch)));

            if(!didSneakOrElytra) {
                to.y+= 1.62f;
                for (Vector direction : directions) {
                    for (SimpleCollisionBox targetBox : boxes) {
                        final AxisAlignedBB vanillaBox = new AxisAlignedBB(targetBox);

                        Vec3D intersectTo = vanillaBox.rayTrace(to.toVector(), direction, 10);

                        if(intersectTo != null) {
                            lastAimOnTarget.reset();
                            hits++;
                            distance = Math.min(distance, intersectTo.distanceSquared(new Vec3D(to.x, to.y, to.z)));
                            collided = true;
                        }
                    }
                }
                //Checking all possible eyeheights since client actions notoriously desync from the server side
            } else {
                for (Vector direction : directions) {
                    for (double eyeHeight : getPlayer().getMovement().getEyeHeights()) {
                        for (SimpleCollisionBox targetBox : boxes) {
                            final AxisAlignedBB vanillaBox = new AxisAlignedBB(targetBox);

                            KLocation from = to.clone();

                            from.y+= eyeHeight;
                            Vec3D intersectTo = vanillaBox.rayTrace(from.toVector(), direction, 10);

                            if(intersectTo != null) {
                                lastAimOnTarget.reset();
                                hits++;
                                distance = Math.min(distance, intersectTo.distanceSquared(new Vec3D(from.x, from.y, from.z)));
                                collided = true;
                            }
                        }
                    }
                }
            }

            if(collided) {
                hbuffer = 0;
                distance = Math.sqrt(distance);
                if(distance > 3.05) {
                    if(++buffer > 2) {
                        flag("d=%.3f>-3.05", distance);
                        buffer = Math.min(2, buffer);
                    }
                } else if(buffer > 0) buffer-= 0.05f;

                if(hbuffer > 0) hbuffer--;

                debug("buffer: %.3f distance=%.2f hits=%s", buffer, distance, hits);
            } else {
                if (++hbuffer > 5) {
                    flag("%.1f;%.1f;%.1f", eloc.x, eloc.y, eloc.z);
                }
                debug("Missed!");
            }
        }
    }

}
