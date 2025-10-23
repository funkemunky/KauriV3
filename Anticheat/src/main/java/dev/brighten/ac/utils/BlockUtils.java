package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.handler.entity.TrackedEntity;

import java.util.Map;
import java.util.Optional;

public class BlockUtils {

    public static Optional<WrappedBlock> getRelative(APlayer player, Vector3i location, int modX, int modY, int modZ) {
        return Optional.of(player.getBlockUpdateHandler()
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

    private static final Map<ItemType, Float> FRICTION = Map.of(
            ItemTypes.SLIME_BLOCK, 0.8f,
            ItemTypes.ICE, 0.98f,
            ItemTypes.PACKED_ICE, 0.98f,
            ItemTypes.BLUE_ICE, 0.989f
    );

    private static final Map<StateType, Float> STATE_FRICTION = Map.of(
            StateTypes.ICE, 0.98f,
            StateTypes.PACKED_ICE, 0.98f,
            StateTypes.FROSTED_ICE, 0.98f,
            StateTypes.SLIME_BLOCK, 0.8f,
            StateTypes.HONEY_BLOCK, 0.8f
    );

    public static float getFriction(ItemType material) {
        return FRICTION.getOrDefault(material, 0.6f);
    }


    public static float getMaterialFriction(ClientVersion version, StateType material) {

        if (material == StateTypes.BLUE_ICE) {
            if (version.isNewerThanOrEquals(ClientVersion.V_1_13))
                return 0.98f;

            return 0.98f;
        }

        return STATE_FRICTION.getOrDefault(material, 0.6f);
    }

    public static boolean isEntityCollidable(TrackedEntity entity) {
        return entity.getEntityType().isInstanceOf(EntityTypes.BOAT) ||
                entity.getEntityType().isInstanceOf(EntityTypes.MINECART);
    }

    @SuppressWarnings("deprecation")
    public static boolean isSolid(ItemType material, ClientVersion version) {
        int type = material.getId(version);

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

