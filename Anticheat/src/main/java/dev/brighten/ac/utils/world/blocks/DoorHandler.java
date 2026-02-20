package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Hinge;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.HexCollisionBox;
import dev.brighten.ac.utils.world.types.NoCollisionBox;

// https://github.com/GrimAnticheat/Grim/blob/5de34f52f3a8a71f9b1304f528c1f1601dadf5b4/common/src/main/java/ac/grim/grimac/utils/collisions/blocks/DoorHandler.java
public class DoorHandler implements CollisionFactory {
    protected static final CollisionBox SOUTH_AABB =
            new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final CollisionBox NORTH_AABB =
            new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox WEST_AABB =
            new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox EAST_AABB =
            new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock b) {
        return switch (fetchDirection(player, version, b)) {
            case NORTH -> NORTH_AABB.copy();
            case SOUTH -> SOUTH_AABB.copy();
            case EAST -> EAST_AABB.copy();
            case WEST -> WEST_AABB.copy();
            default -> NoCollisionBox.INSTANCE;
        };
    }

    public BlockFace fetchDirection(APlayer player, ClientVersion version, WrappedBlock door) {
        BlockFace facingDirection;
        boolean isClosed;
        boolean isRightHinge;

        // 1.12 stores block data for the top door in the bottom block data
        // ViaVersion can't send 1.12 clients the 1.13 complete data
        // For 1.13, ViaVersion should just use the 1.12 block data
        // I hate legacy versions... this is so messy
        //TODO: This needs to be updated to support corrupted door collision
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2)
                || version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            if (door.getBlockState().getHalf() == Half.LOWER) {
                Vector3i aboveVec = door.getLocation();

                aboveVec = aboveVec.add(0, aboveVec.getY() + 1, 0);
                WrappedBlockState above = player.getWorldTracker().getBlock(aboveVec).getBlockState();

                facingDirection = door.getBlockState().getFacing();
                isClosed = !door.getBlockState().isOpen();

                // Doors have to be the same material in 1.12 for their block data to be connected together
                // For example, if you somehow manage to get a jungle top with an oak bottom, the data isn't shared
                if (above.getType() == door.getBlockState().getType()) {
                    isRightHinge = above.getHinge() == Hinge.RIGHT;
                } else {
                    // Default missing value
                    isRightHinge = false;
                }
            } else {
                Vector3i belowVec = door.getLocation();

                belowVec = belowVec.add(0, belowVec.getY() - 1, 0);
                WrappedBlockState below = player.getWorldTracker().getBlock(belowVec).getBlockState();

                if (below.getType() == door.getBlockState().getType() && below.getHalf() == Half.LOWER) {
                    isClosed = !below.isOpen();
                    facingDirection = below.getFacing();
                    isRightHinge = door.getBlockState().getHinge() == Hinge.RIGHT;
                } else {
                    facingDirection = BlockFace.EAST;
                    isClosed = true;
                    isRightHinge = false;
                }
            }
        } else {
            facingDirection = door.getBlockState().getFacing();
            isClosed = !door.getBlockState().isOpen();
            isRightHinge = door.getBlockState().getHinge() == Hinge.RIGHT;
        }

        return switch (facingDirection) {
            case SOUTH ->
                    isClosed ? BlockFace.SOUTH : (isRightHinge ? BlockFace.EAST : BlockFace.WEST);
            case WEST ->
                    isClosed ? BlockFace.WEST : (isRightHinge ? BlockFace.SOUTH : BlockFace.NORTH);
            case NORTH ->
                    isClosed ? BlockFace.NORTH : (isRightHinge ? BlockFace.WEST : BlockFace.EAST);
            default ->
                    isClosed ? BlockFace.EAST : (isRightHinge ? BlockFace.NORTH : BlockFace.SOUTH);
        };
    }
}
