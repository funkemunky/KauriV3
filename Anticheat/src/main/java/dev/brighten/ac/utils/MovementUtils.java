package dev.brighten.ac.utils;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MovementUtils {

    private static Enchantment DEPTH;

    public static double getJumpHeight(APlayer data) {
        float baseHeight = 0.42f;

        baseHeight+= data.getInfo().groundJumpBoost.map(ef -> ef.getAmplifier() + 1)
                .orElse(0) * 0.1f;

        return baseHeight;
    }

    public static boolean isSameLocation(KLocation one, KLocation two) {
        return one.x == two.x && one.y == two.y && one.z == two.z;
    }

    public static boolean isOnLadder(APlayer data) {
        try {
            int i = MathHelper.floor_double(data.getMovement().getTo().getLoc().x);
            int j = MathHelper.floor_double(data.getMovement().getTo().getBox().minY);
            int k = MathHelper.floor_double(data.getMovement().getTo().getLoc().z);
            Block block = BlockUtils.getBlock(new Location(data.getBukkitPlayer().getWorld(), i, j, k));

            return Materials.checkFlag(block.getType(), Materials.LADDER);
        } catch(NullPointerException e) {
            return false;
        }
    }

    private static final WrappedField checkMovement = ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)
            ? MinecraftReflection.playerConnection.getFieldByName("checkMovement")
            : MinecraftReflection.playerConnection.getFieldByName(ProtocolVersion.getGameVersion()
            .isOrAbove(ProtocolVersion.V1_17) ? "y" : "teleportPos");
    public static boolean checkMovement(Object playerConnection) {
        if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
            return checkMovement.get(playerConnection);
        } else return (checkMovement.get(playerConnection) == null);
    }

    public static int getDepthStriderLevel(Player player) {
        if(DEPTH == null) return 0;

        val boots = player.getInventory().getBoots();

        if(boots == null) return 0;

        return boots.getEnchantmentLevel(DEPTH);
    }

    public static double getHorizontalDistance(KLocation one, KLocation two) {
        return MathUtils.hypot(one.x - two.x, one.z - two.z);
    }

    public static float getFriction(Block block) {
        XMaterial matched = BlockUtils.getXMaterial(block.getType());

        if(matched == null) return 0.6f;
        switch(matched) {
            case SLIME_BLOCK:
                return 0.8f;
            case ICE:
            case BLUE_ICE:
            case FROSTED_ICE:
            case PACKED_ICE:
                return 0.98f;
            default:
                return 0.6f;
        }
    }

    public static Location findGroundLocation(APlayer player, int lowestBelow) {
        // Player is on the ground, so no need to calculate
        if(player.getInfo().isServerGround()) {
            return player.getMovement().getTo().getLoc().toLocation(player.getBukkitPlayer().getWorld());
        }
        int x = MathHelper.floor_double(player.getMovement().getTo().getX()),
                startY = MathHelper.floor_double(player.getMovement().getTo().getY()),
                z = MathHelper.floor_double(player.getMovement().getTo().getZ());

        for(int y = startY ; y > startY - lowestBelow ; y--) {
            val block = BlockUtils
                    .getBlockAsync(new Location(player.getBukkitPlayer().getWorld(), x, y, z));

            if(!block.isPresent()) break; //No point in continuing since the one below will still be not present.

            if(Materials.checkFlag(block.get().getType(), Materials.SOLID)
                    && Materials.checkFlag(block.get().getType(), Materials.LIQUID)) {
                CollisionBox box = BlockData.getData(block.get().getType())
                        .getBox(block.get(), ProtocolVersion.getGameVersion());

                if(box instanceof SimpleCollisionBox) {
                    SimpleCollisionBox sbox = (SimpleCollisionBox) box;

                    return new Location(block.get().getWorld(), x, sbox.maxY, z);
                } else {
                    List<SimpleCollisionBox> sboxes = new ArrayList<>();

                    box.downCast(sboxes);

                    double maxY = sboxes.stream().max(Comparator.comparing(sbox -> sbox.maxY)).map(s -> s.maxY)
                            .orElse(y + 1.);

                    return new Location(block.get().getWorld(), x, maxY, z);
                }
            }
        }

        return new Location(player.getBukkitPlayer().getWorld(), player.getMovement().getTo().getX(), startY, player.getMovement().getTo().getZ());
    }

    public static Location findGroundLocation(Location toStart, int lowestBelow) {
        // Player is on the ground, so no need to calculate
        int x = MathHelper.floor_double(toStart.getX()),
                startY = MathHelper.floor_double(toStart.getY()),
                z = MathHelper.floor_double(toStart.getZ());

        for(int y = startY ; y > startY - lowestBelow ; y--) {
            val block = BlockUtils
                    .getBlockAsync(new Location(toStart.getWorld(), x, y, z));

            if(!block.isPresent()) break; //No point in continuing since the one below will still be not present.

            if(Materials.checkFlag(block.get().getType(), Materials.SOLID)
                    || Materials.checkFlag(block.get().getType(), Materials.LIQUID)) {
                CollisionBox box = BlockData.getData(block.get().getType())
                        .getBox(block.get(), ProtocolVersion.getGameVersion());

                if(box instanceof SimpleCollisionBox) {
                    SimpleCollisionBox sbox = (SimpleCollisionBox) box;

                    return new Location(block.get().getWorld(), x, sbox.maxY, z);
                } else {
                    List<SimpleCollisionBox> sboxes = new ArrayList<>();

                    box.downCast(sboxes);

                    double maxY = sboxes.stream().max(Comparator.comparing(sbox -> sbox.maxY)).map(s -> s.maxY)
                            .orElse(y + 0.01);

                    return new Location(block.get().getWorld(), x, maxY, z);
                }
            }
        }

        return new Location(toStart.getWorld(), toStart.getX(), startY, toStart.getZ());
    }

    public static float getTotalHeight(float initial) {
        return getTotalHeight(ProtocolVersion.V1_8_9, initial);
    }

    public static float getTotalHeight(ProtocolVersion version, float initial) {
        float nextCalc = initial, total = initial;
        int count = 0;
        while ((nextCalc = (nextCalc - 0.08f) * 0.98f) > (version.isOrBelow(ProtocolVersion.V1_8_9) ?  0.005 : 0)) {
            total+= nextCalc;
            if(count++ > 15) {
                return total * 4;
            }
        }

        return total;
    }

    static {
        try {
            if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_8)) {
                DEPTH = Enchantment.getByName("DEPTH_STRIDER");
            }

            String test = "%%__USER__%%";
        } catch(Exception e) {
            DEPTH = null;
        }
    }
}
