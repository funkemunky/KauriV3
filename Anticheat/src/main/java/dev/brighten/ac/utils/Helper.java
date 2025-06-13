package dev.brighten.ac.utils;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutWorldParticles;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author DeprecatedLuke
 * Taken from https://github.com/DeprecatedLuke/fireflyx/blob/master/src/main/java/com/ngxdev/anticheat/utils/Helper.java
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

    public static void drawRay(RayCollision collision, double distance, EnumParticle particle, Collection<? extends Player> players) {
        for (double i = 0; i < distance; i += 0.2) {
            float fx = (float) (collision.originX + (collision.directionX * i));
            float fy = (float) (collision.originY + (collision.directionY * i));
            float fz = (float) (collision.originZ + (collision.directionZ * i));

            WPacketPlayOutWorldParticles packet = WPacketPlayOutWorldParticles.builder()
                    .particle(particle)
                    .x(fx)
                    .y(fy)
                    .z(fz)
                    .offsetX(0)
                    .offsetY(0)
                    .offsetZ(0)
                    .speed(0)
                    .amount(1)
                    .longD(true)
                    .data(new int[0])
                    .build();
            players.forEach(p -> HandlerAbstract.getHandler().sendPacketSilently(p, packet));
        }
    }


    public static void drawCuboid(SimpleCollisionBox box, EnumParticle particle, Collection<? extends Player> players) {
        Step.GenericStepper<Float> x = Step.step((float) box.minX, 0.241F, (float) box.maxX);
        Step.GenericStepper<Float> y = Step.step((float) box.minY, 0.241F, (float) box.maxY);
        Step.GenericStepper<Float> z = Step.step((float) box.minZ, 0.241F, (float) box.maxZ);
        Iterator var6 = x.iterator();

        while (var6.hasNext()) {
            float fx = (Float) var6.next();
            Iterator var8 = y.iterator();

            label61:
            while (var8.hasNext()) {
                float fy = (Float) var8.next();
                Iterator var10 = z.iterator();

                while (true) {
                    float fz;
                    int check;
                    do {
                        if (!var10.hasNext()) {
                            continue label61;
                        }

                        fz = (Float) var10.next();
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

                    WPacketPlayOutWorldParticles packet = WPacketPlayOutWorldParticles.builder()
                            .particle(particle)
                            .x(fx)
                            .y(fy)
                            .z(fz)
                            .offsetX(0)
                            .offsetY(0)
                            .offsetZ(0)
                            .speed(0)
                            .amount(1)
                            .data(new int[0])
                            .build();

                    Iterator<? extends Player> var14 = players.iterator();

                    while (var14.hasNext()) {
                        Player p = var14.next();
                        HandlerAbstract.getHandler().sendPacketSilently(p, packet);
                    }
                }
            }
        }

    }

    public static void drawPoint(Vector point, EnumParticle particle, Collection<? extends Player> players) {
        WPacketPlayOutWorldParticles packet = WPacketPlayOutWorldParticles.builder()
                .particle(particle)
                .x((float) point.getX())
                .y((float) point.getY())
                .z((float) point.getZ())
                .offsetX(0)
                .offsetY(0)
                .offsetZ(0)
                .speed(0)
                .amount(1)
                .data(new int[0])
                .build();
        Iterator<? extends Player> var4 = players.iterator();

        while (var4.hasNext()) {
            Player p = var4.next();
            HandlerAbstract.getHandler().sendPacketSilently(p, packet);
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

    public static List<Block> blockCollisions(List<Block> blocks, CollisionBox box) {
        return blocks.stream()
                .filter(b -> BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion()).isCollided(box))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static boolean isCollided(SimpleCollisionBox toCheck, CollisionBox other) {
        List<SimpleCollisionBox> downcasted = new ArrayList<>();

        other.downCast(downcasted);

        return downcasted.stream().anyMatch(box -> box.maxX >= toCheck.minX && box.minX <= toCheck.maxX
                && box.maxY >= toCheck.minY && box.minY <= toCheck.maxY && box.maxZ >= toCheck.minZ
                && box.minZ <= toCheck.maxZ);
    }

    public static List<Block> blockCollisions(List<Block> blocks, CollisionBox box, int material) {
        return blocks.stream().filter(b -> Materials.checkFlag(b.getType(), material))
                .filter(b -> BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion()).isCollided(box))
                .collect(Collectors.toCollection(LinkedList::new));
    }


    public static <C extends CollisionBox> List<C> collisions(List<C> boxes, CollisionBox box) {
        return boxes.stream().filter(b -> b.isCollided(box))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<SimpleCollisionBox> getCollisions(World world, SimpleCollisionBox collisionBox, int mask) {
        int x1 = (int) Math.floor(collisionBox.minX);
        int y1 = (int) Math.floor(collisionBox.minY);
        int z1 = (int) Math.floor(collisionBox.minZ);
        int x2 = (int) Math.floor(collisionBox.maxX + 1);
        int y2 = (int) Math.floor(collisionBox.maxY + 1);
        int z2 = (int) Math.floor(collisionBox.maxZ + 1);
        List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();
        Block block;
        for (int x = x1; x < x2; ++x)
            for (int y = y1 - 1; y < y2; ++y)
                for (int z = z1; z < z2; ++z)
                    if ((block = getBlockAt(world, x, y, z)) != null
                            && BlockUtils.getXMaterial(block.getType()) != XMaterial.AIR)
                        if (Materials.checkFlag(block.getType(), mask)) {
                            CollisionBox box = BlockData.getData(block.getType())
                                    .getBox(block, ProtocolVersion.getGameVersion());

                            if (box.isIntersected(collisionBox)) {
                                box.downCast(collisionBoxes);
                            }
                        }
        return collisionBoxes;
    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, SimpleCollisionBox collisionBox) {
        return getCollisions(player, collisionBox, Materials.COLLIDABLE);
    }

    public static SimpleCollisionBox getEntityCollision(Entity entity) {
        return new SimpleCollisionBox(MinecraftReflection.getEntityBoundingBox(entity));
    }

    public static List<SimpleCollisionBox> getCollisions(APlayer player, SimpleCollisionBox collisionBox, int mask) {
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
                    Material type = player.getBlockUpdateHandler().getBlock(vec).getType();

                    if (type != Material.AIR && Materials.checkFlag(type, mask)) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, ProtocolVersion.getGameVersion());

                        if (box.isIntersected(collisionBox)) {
                            box.downCast(collisionBoxes);
                        }
                    }
                }

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

    public static List<Tuple<SimpleCollisionBox, Material>>
    getCollisionsWithTypeNoEntities(APlayer player, SimpleCollisionBox collisionBox) {
        return getCollisionsWithTypeNoEntities(player, collisionBox, Materials.COLLIDABLE);
    }

    public static List<Tuple<SimpleCollisionBox, Material>>
    getCollisionsWithTypeNoEntities(APlayer player, SimpleCollisionBox collisionBox, int mask) {
        int x1 = (int) Math.floor(collisionBox.minX);
        int y1 = (int) Math.floor(collisionBox.minY);
        int z1 = (int) Math.floor(collisionBox.minZ);
        int x2 = (int) Math.floor(collisionBox.maxX + 1);
        int y2 = (int) Math.floor(collisionBox.maxY + 1);
        int z2 = (int) Math.floor(collisionBox.maxZ + 1);
        List<Tuple<SimpleCollisionBox, Material>> collisionBoxes = new ArrayList<>();
        for (int x = x1; x < x2; ++x)
            for (int y = y1 - 1; y < y2; ++y)
                for (int z = z1; z < z2; ++z) {
                    IntVector vec = new IntVector(x, y, z);
                    Material type = player.getBlockUpdateHandler().getBlock(vec).getType();

                    if (type != Material.AIR && Materials.checkFlag(type, mask)) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, ProtocolVersion.getGameVersion());

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
                    Material type = player.getBlockUpdateHandler().getBlock(vec).getType();

                    if (type != Material.AIR && Materials.checkFlag(type, mask)) {
                        CollisionBox box = BlockData.getData(type)
                                .getBox(player, vec, ProtocolVersion.getGameVersion());

                        if (box.isIntersected(collisionBox)) {
                            box.downCast(collisionBoxes);
                        }
                    }
                }

        return collisionBoxes;
    }

    public static List<Block> getBlocksNearby2(World world, SimpleCollisionBox collisionBox, int mask) {
        int x1 = (int) Math.floor(collisionBox.minX);
        int y1 = (int) Math.floor(collisionBox.minY);
        int z1 = (int) Math.floor(collisionBox.minZ);
        int x2 = (int) Math.ceil(collisionBox.maxX);
        int y2 = (int) Math.ceil(collisionBox.maxY);
        int z2 = (int) Math.ceil(collisionBox.maxZ);
        List<Block> blocks = new LinkedList<>();
        Block block;
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    if ((block = getBlockAt(world, x, y, z)) != null
                            && BlockUtils.getXMaterial(block.getType()) != XMaterial.AIR)
                        if (Materials.checkFlag(block.getType(), mask))
                            blocks.add(block);
        return blocks;
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

    public static List<CollisionBox> toCollisions(List<Block> blocks) {
        return blocks.stream().map(b -> BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<SimpleCollisionBox> toCollisionsDowncasted(List<Block> blocks) {
        List<SimpleCollisionBox> collisions = new LinkedList<>();
        blocks.forEach(b -> BlockData.getData(b.getType())
                .getBox(b, ProtocolVersion.getGameVersion()).downCast(collisions));
        return collisions;
    }

    public static CollisionBox toCollisions(Block b) {
        return BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion());
    }
}
