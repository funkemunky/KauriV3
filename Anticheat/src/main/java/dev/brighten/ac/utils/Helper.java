package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                p.sendPacketSilently(packet);
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
                float fy = (Float) var8.next();
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
                        p.sendPacketSilently(packet);
                    }
                }
            }
        }

    }

    public static void drawPoint(Vector3d point, ParticleType<?> particle, Collection<? extends APlayer> players) {
        WrapperPlayServerParticle packet = new WrapperPlayServerParticle(new Particle<>(particle), false,
                point, new Vector3f(0, 0, 0),  0f, 1);

        for (APlayer p : players) {
            p.sendPacketSilently(packet);
        }
    }

    public static Block getBlockAt(World world, int x, int y, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4)
                ? world.getChunkAt(x >> 4, z >> 4).getBlock(x & 15, y, z & 15)
                : null;
    }

    public static SimpleCollisionBox wrap(SimpleCollisionBox a, SimpleCollisionBox b) {
        double minX = Math.min(a.minX, b.minX);
        double minY = Math.min(a.minY, b.minY);
        double minZ = Math.min(a.minZ, b.minZ);
        double maxX = Math.max(a.maxX, b.maxX);
        double maxY = Math.max(a.maxY, b.maxY);
        double maxZ = Math.max(a.maxZ, b.maxZ);
        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static SimpleCollisionBox wrap(List<SimpleCollisionBox> box) {
        if (!box.isEmpty()) {
            SimpleCollisionBox wrap = box.get(0).copy();
            for (int i = 1; i < box.size(); i++) {
                SimpleCollisionBox a = box.get(i);
                if (wrap.minX > a.minX) wrap.minX = a.minX;
                if (wrap.minY > a.minY) wrap.minY = a.minY;
                if (wrap.minZ > a.minZ) wrap.minZ = a.minZ;
                if (wrap.maxX < a.maxX) wrap.maxX = a.maxX;
                if (wrap.maxY < a.maxY) wrap.maxY = a.maxY;
                if (wrap.maxZ < a.maxZ) wrap.maxZ = a.maxZ;
            }
            return wrap;
        }
        return null;
    }

    public static boolean isCollided(SimpleCollisionBox toCheck, CollisionBox other) {
        List<SimpleCollisionBox> downcasted = new ArrayList<>();

        other.downCast(downcasted);

        return downcasted.stream().anyMatch(box -> box.maxX >= toCheck.minX && box.minX <= toCheck.maxX
                && box.maxY >= toCheck.minY && box.minY <= toCheck.maxY && box.maxZ >= toCheck.minZ
                && box.minZ <= toCheck.maxZ);
    }


    public static <C extends CollisionBox> List<C> collisions(List<C> boxes, CollisionBox box) {
        return boxes.stream().filter(b -> b.isCollided(box))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, SimpleCollisionBox collisionBox) {
        return getCollisions(player, collisionBox, Materials.COLLIDABLE);
    }

    public static SimpleCollisionBox getEntityCollision(Entity entity) {
        return new SimpleCollisionBox(MinecraftReflection.getEntityBoundingBox(entity));
    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, SimpleCollisionBox collisionBox, int mask) {
        List<SimpleCollisionBox> collisionBoxes = getCollisionsNoEntities(player, collisionBox, mask);

        if(player != null) {
            for (Entity entity : player.getInfo().getNearbyEntities()) {
                if (!BlockUtils.isEntityCollidable(entity)) continue;

                SimpleCollisionBox entityCollisionBox =
                        new SimpleCollisionBox(MinecraftReflection.getEntityBoundingBox(entity));

                if (entityCollisionBox.isIntersected(collisionBox))
                    entityCollisionBox.downCast(collisionBoxes);
            }
        }

        return collisionBoxes;
    }

    public static List<Tuple<SimpleCollisionBox, StateType>>
    getCollisionsWithTypeNoEntities(APlayer player, SimpleCollisionBox collisionBox) {
        return getCollisionsWithTypeNoEntities(player, collisionBox, Materials.COLLIDABLE);
    }

    public static List<Tuple<SimpleCollisionBox, StateType>>
    getCollisionsWithTypeNoEntities(APlayer player, SimpleCollisionBox collisionBox, int mask) {
        int x1 = (int) Math.floor(collisionBox.minX);
        int y1 = (int) Math.floor(collisionBox.minY);
        int z1 = (int) Math.floor(collisionBox.minZ);
        int x2 = (int) Math.floor(collisionBox.maxX + 1);
        int y2 = (int) Math.floor(collisionBox.maxY + 1);
        int z2 = (int) Math.floor(collisionBox.maxZ + 1);
        List<Tuple<SimpleCollisionBox, StateType>> collisionBoxes = new ArrayList<>();
        for (int x = x1; x < x2; ++x)
            for (int y = y1 - 1; y < y2; ++y)
                for (int z = z1; z < z2; ++z) {
                    IntVector vec = new IntVector(x, y, z);
                    StateType type = player.getBlockUpdateHandler().getBlock(vec).getType();

                    if (type != StateTypes.AIR && Materials.checkFlag(type, mask)) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

                        if (box.isIntersected(collisionBox)) {
                            for (SimpleCollisionBox simpleCollisionBox : box.downCast()) {
                                collisionBoxes.add(new Tuple<>(simpleCollisionBox, type));
                            }
                        }
                    }
                }

        return collisionBoxes;
    }

    public static List<SimpleCollisionBox> getCollisionsNoEntities(APlayer player,
                                                                   SimpleCollisionBox collisionBox, int mask) {
        int x1 = (int) Math.floor(collisionBox.minX);
        int y1 = (int) Math.floor(collisionBox.minY);
        int z1 = (int) Math.floor(collisionBox.minZ);
        int x2 = (int) Math.floor(collisionBox.maxX + 1);
        int y2 = (int) Math.floor(collisionBox.maxY + 1);
        int z2 = (int) Math.floor(collisionBox.maxZ + 1);
        List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();
        for (int x = x1; x < x2; ++x)
            for (int y = y1 - 1; y < y2; ++y)
                for (int z = z1; z < z2; ++z) {
                    IntVector vec = new IntVector(x, y, z);
                    StateType type = player.getBlockUpdateHandler().getBlock(vec).getType();

                    if (type != StateTypes.AIR && Materials.checkFlag(type, mask)) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

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

    public static String drawUsage(long max, long time) {
        double chunk = max / 50.;
        String line = IntStream.range(0, 50).mapToObj(i -> (chunk * i < time ? "§c" : "§7") + "❘")
                .collect(Collectors.joining("", "[", ""));
        String zeros = "00";
        String nums = Integer.toString((int) ((time / (double) max) * 100));
        return line + "§f] §c" + zeros.substring(0, 3 - nums.length()) + nums + "% §f❘";
    }

    public static String drawUsage(long max, double time) {
        double chunk = max / 50.;
        String line = IntStream.range(0, 50).mapToObj(i -> (chunk * i < time ? "§c" : "§7") + "❘")
                .collect(Collectors.joining("", "[", ""));
        String nums = String.valueOf(format((time / (double) max) * 100, 3));
        return line + "§f] §c" + nums + "%";
    }
}
