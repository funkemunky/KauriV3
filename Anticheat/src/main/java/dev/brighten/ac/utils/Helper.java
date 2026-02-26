package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * @author DeprecatedLuke
 * Taken from <a href="https://github.com/DeprecatedLuke/fireflyx/blob/master/src/main/java/com/ngxdev/anticheat/utils/Helper.java">...</a>
 */
public class Helper {

    public static Vector vector(double yaw, double pitch) {
        Vector vector = new Vector();
        vector.setY(-Math.sin(Math.toRadians(pitch)));
        double xz = Math.cos(Math.toRadians(pitch));
        vector.setX(-xz * Math.sin(Math.toRadians(yaw)));
        vector.setZ(xz * Math.cos(Math.toRadians(yaw)));
        return vector;
    }

    public static void drawRay(RayCollision collision, double distance, ParticleType<?> particle, Collection<? extends APlayer> players) {
        for (double i = 0; i < distance; i += 0.2) {
            float fx = (float) (collision.originX + (collision.directionX * i));
            float fy = (float) (collision.originY + (collision.directionY * i));
            float fz = (float) (collision.originZ + (collision.directionZ * i));

            WrapperPlayServerParticle packet = new WrapperPlayServerParticle(new Particle<>(particle), false,
                    new Vector3d(fx, fy, fz), new Vector3f(0, 0, 0),  0f, 1);

            for (APlayer p : players) {
                p.writePacketSilently(packet);
            }
        }
    }


    public static void drawCuboid(SimpleCollisionBox box, ParticleType<?> particle, Collection<? extends APlayer> players) {
        Step.GenericStepper<Float> x = Step.step((float) box.minX, 0.241F, (float) box.maxX);
        Step.GenericStepper<Float> y = Step.step((float) box.minY, 0.241F, (float) box.maxY);
        Step.GenericStepper<Float> z = Step.step((float) box.minZ, 0.241F, (float) box.maxZ);

        for (float fx : x) {
            Iterator<Float> var8 = y.iterator();

            label61:
            while (var8.hasNext()) {
                float fy = var8.next();
                Iterator<Float> var10 = z.iterator();

                while (true) {
                    float fz;
                    int check;
                    do {
                        if (!var10.hasNext()) {
                            continue label61;
                        }

                        fz = var10.next();
                        check = 0;
                        if (x.first() || x.last()) {
                            ++check;
                        }

                        if (y.first() || y.last()) {
                            ++check;
                        }

                        if (z.first() || z.last()) {
                            ++check;
                        }
                    } while (check < 2);

                    WrapperPlayServerParticle packet = new WrapperPlayServerParticle(new Particle<>(particle), false,
                            new Vector3d(fx, fy, fz), new Vector3f(0, 0, 0), 0f, 1);

                    for (APlayer p : players) {
                        p.sendPacket(packet);
                    }
                }
            }
        }

    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, CollisionBox collisionBox) {
        return getCollisions(player, collisionBox, -100);
    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, CollisionBox collisionBox, int mask) {
        List<SimpleCollisionBox> collisionBoxes = getCollisionsNoEntities(player, collisionBox, mask);

        if(player != null) {
            for (TrackedEntity entity : player.getInfo().getNearbyEntities()) {
                if (!BlockUtils.isEntityCollidable(entity)) continue;

                CollisionBox entityCollisionBox = EntityData.getEntityBox(entity.getLocation()
                        .toLocation(player.getBukkitPlayer().getWorld()), entity);

                if (entityCollisionBox.isIntersected(collisionBox))
                    entityCollisionBox.downCast(collisionBoxes);
            }
        }

        return collisionBoxes;
    }

    public static List<SimpleCollisionBox> getCollisionsNoEntities(APlayer player,
                                                                   CollisionBox collisionBox, int mask) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        if(collisionBox instanceof SimpleCollisionBox box) {
            minX = box.minX;
            minY = box.minY;
            minZ = box.minZ;
            maxX = box.maxX;
            maxY = box.maxY;
            maxZ = box.maxZ;
        } else {
            for (SimpleCollisionBox box : collisionBox.downCast()) {
                minX = Math.min(minX, box.minX);
                minY = Math.min(minY, box.minY);
                minZ = Math.min(minZ, box.minZ);
                maxX = Math.max(maxX, box.maxX);
                maxY = Math.max(maxY, box.maxY);
                maxZ = Math.max(maxZ, box.maxZ);
            }
        }
        int x1 = (int) Math.floor(minX);
        int y1 = (int) Math.floor(minY);
        int z1 = (int) Math.floor(minZ);
        int x2 = (int) Math.floor(maxX + 1);
        int y2 = (int) Math.floor(maxY + 1);
        int z2 = (int) Math.floor(maxZ + 1);
        List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();
        for (int x = x1; x < x2; ++x)
            for (int y = y1 - 1; y < y2; ++y)
                for (int z = z1; z < z2; ++z) {
                    Vector3i vec = new Vector3i(x, y, z);
                    StateType type = player.getWorldTracker().getBlock(vec).getType();

                    if (type != StateTypes.AIR && (mask == -100 || Materials.checkFlag(type, mask))) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, player.getPlayerVersion());

                        if (box.isIntersected(collisionBox)) {
                            box.downCast(collisionBoxes);
                        }
                    }
                }

        return collisionBoxes;
    }

    private static final int[] decimalPlaces = {0, 10, 100, 1000, 10000, 100000, 1000000,
            10000000, 100000000, 1000000000};

    public static double format(double d, int dec) {
        return (long) (d * decimalPlaces[dec] + 0.5) / (double) decimalPlaces[dec];
    }
}
