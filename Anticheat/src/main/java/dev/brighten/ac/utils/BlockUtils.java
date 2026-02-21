package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.handler.entity.TrackedEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Optional;

public class BlockUtils {
    private static final EnumMap<Material, XMaterial> matchMaterial = new EnumMap<>(Material.class);
    private static final EnumSet<Material> SWORDS = EnumSet.allOf(Material.class),
            EDIBLE = EnumSet.allOf(Material.class),
            DOOR = EnumSet.allOf(Material.class);
    private static final EnumSet<XMaterial> XEDIBLE = EnumSet.allOf(XMaterial.class);

    static {
        for (Material mat : Material.values()) {
            XMaterial xmat = XMaterial.matchXMaterial(mat);
            matchMaterial.put(mat, xmat);

            if(mat.toString().contains("SWORD")) {
                SWORDS.add(mat);
            } else if(mat.toString().contains("DOOR") && !mat.toString().contains("TRAP")) {
                DOOR.add(mat);
            }

            if(mat.isEdible()) {
                EDIBLE.add(mat);
                XEDIBLE.add(xmat);
            }
        }
    }

    public static XMaterial getXMaterial(Material material) {
        return matchMaterial.get(material);
    }

    public static XMaterial getXMaterial(ItemType itemType) {
        return matchMaterial.get(SpigotConversionUtil.toBukkitItemMaterial(itemType));
    }

    @Deprecated
    public static Block getBlock(Location location) {
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return location.getBlock();
        } else {
            return null;
        }
    }

    public static boolean isSword(Material material) {
        return SWORDS.contains(material);
    }

    public static Optional<Block> getBlockAsync(Location location) {
        if(Bukkit.isPrimaryThread()
                || location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))  {
            return Optional.of(location.getBlock());
        }

        return Optional.empty();
    }

    public static Optional<Chunk> getChunkAsync(World world, int x, int z) {
        if(Bukkit.isPrimaryThread()
                || world.isChunkLoaded(x, z))  {
            return Optional.of(world.getChunkAt(x, z));
        }

        return Optional.empty();
    }

    public static Optional<WrappedBlock> getRelative(APlayer player, Vector3i location, int modX, int modY, int modZ) {
        return Optional.of(player.getWorldTracker()
                .getRelative(new Vector3i(location.getX(), location.getY(), location.getZ()),
                        modX, modY, modZ));
    }

    public static Optional<WrappedBlock> getRelative(APlayer player, Vector3i location,
                                                     com.github.retrooper.packetevents.protocol.world
                                                             .BlockFace face, int distance) {
        return getRelative(player, location,
                face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    public static Optional<WrappedBlock> getRelative(APlayer player, Vector3i vector, BlockFace face) {
        return getRelative(player, vector,
                face.getModX(), face.getModY(), face.getModZ());
    }

    public static float getFriction(XMaterial material) {
        return switch (material) {
            case SLIME_BLOCK -> 0.8f;
            case ICE, PACKED_ICE, FROSTED_ICE -> 0.98f;
            case BLUE_ICE -> 0.989f;
            default -> 0.6f;
        };
    }
    public static float getMaterialFriction(APlayer player, StateType material) {
        float friction = 0.6f;

        if (material == StateTypes.ICE) friction = 0.98f;
        if (material == StateTypes.SLIME_BLOCK && player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_8))
            friction = 0.8f;
        // ViaVersion honey block replacement
        if (material == StateTypes.HONEY_BLOCK && player.getPlayerVersion().isOlderThan(ClientVersion.V_1_15))
            friction = 0.8f;
        if (material == StateTypes.PACKED_ICE) friction = 0.98f;
        if (material == StateTypes.FROSTED_ICE) friction = 0.98f;
        if (material == StateTypes.BLUE_ICE) {
            friction = 0.98f;
            if (player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_13))
                friction = 0.989f;
        }

        return friction;
    }

    public static boolean isUsable(Material material) {
        return isUsable(getXMaterial(material));
    }

    public static boolean isUsable(ClientVersion version, ItemType type) {
        return type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE)
                || type == ItemTypes.POTION
                || (version.isOlderThan(ClientVersion.V_1_9) && type.hasAttribute(ItemTypes.ItemAttribute.SWORD))
                || type.equals(ItemTypes.SHIELD);
    }

    public static boolean isUsable(XMaterial xmaterial) {
        if(XEDIBLE.contains(xmaterial)) return true;
        return switch (xmaterial) {
            case STONE_SWORD, DIAMOND_SWORD, GOLDEN_SWORD, IRON_SWORD, WOODEN_SWORD, SHIELD, BOW -> true;
            default -> false;
        };
    }

    public static boolean isEntityCollidable(TrackedEntity entity) {
        return entity.getEntityType().isInstanceOf(EntityTypes.BOAT) ||
                entity.getEntityType().isInstanceOf(EntityTypes.MINECART);
    }

    public static boolean isSolid(Block block) {
        return isSolid(block.getType());
    }

    @SuppressWarnings("deprecation")
    public static boolean isSolid(Material material) {
        int type = material.getId();

        return switch (type) {
            case 1, 2, 3, 4, 5, 7, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 29, 34, 33, 35, 36, 41,
                    42, 43, 44, 45, 46, 47, 48, 49, 52, 53, 54, 56, 57, 58, 60, 61, 62, 64, 65, 67, 71, 73, 74, 78, 79,
                    80, 81, 82, 84, 85, 86, 87, 88, 89, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 106,
                    107, 108, 109, 110, 111, 112, 113, 114, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 127, 128,
                    129, 130, 133, 134, 135, 136, 137, 138, 139, 140, 144, 145, 146, 149, 150, 151, 152, 153, 154, 155,
                    156, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 178, 179,
                    180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199,
                    200, 201, 202, 203, 204, 205, 206, 207, 208, 210, 211, 212, 213, 214, 215, 216, 218, 219, 220, 221,
                    222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241,
                    242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 255, 397, 355 ->
                    true;
            default -> false;
        };
    }

    public static boolean isClimbableBlock(WrappedBlock block) {
        return Materials.checkFlag(block.getType(), Materials.LADDER);
    }
}

