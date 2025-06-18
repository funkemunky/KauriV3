package dev.brighten.ac.utils.world.blocks;


import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.ComplexCollisionBox;
import dev.brighten.ac.utils.world.types.HexCollisionBox;
import org.bukkit.block.BlockFace;

import java.util.Optional;
import java.util.stream.IntStream;

public class DynamicStair implements CollisionFactory {
    protected static final CollisionBox TOP_AABB = new HexCollisionBox(0, 8, 0, 16, 16, 16);
    protected static final CollisionBox BOTTOM_AABB = new HexCollisionBox(0, 0, 0, 16, 8, 16);
    protected static final CollisionBox OCTET_NNN = new HexCollisionBox(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D);
    protected static final CollisionBox OCTET_NNP = new HexCollisionBox(0.0D, 0.0D, 8.0D, 8.0D, 8.0D, 16.0D);
    protected static final CollisionBox OCTET_NPN = new HexCollisionBox(0.0D, 8.0D, 0.0D, 8.0D, 16.0D, 8.0D);
    protected static final CollisionBox OCTET_NPP = new HexCollisionBox(0.0D, 8.0D, 8.0D, 8.0D, 16.0D, 16.0D);
    protected static final CollisionBox OCTET_PNN = new HexCollisionBox(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D);
    protected static final CollisionBox OCTET_PNP = new HexCollisionBox(8.0D, 0.0D, 8.0D, 16.0D, 8.0D, 16.0D);
    protected static final CollisionBox OCTET_PPN = new HexCollisionBox(8.0D, 8.0D, 0.0D, 16.0D, 16.0D, 8.0D);
    protected static final CollisionBox OCTET_PPP = new HexCollisionBox(8.0D, 8.0D, 8.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox[] TOP_SHAPES = makeShapes(TOP_AABB, OCTET_NNN, OCTET_PNN, OCTET_NNP, OCTET_PNP);
    protected static final CollisionBox[] BOTTOM_SHAPES = makeShapes(BOTTOM_AABB, OCTET_NPN, OCTET_PPN, OCTET_NPP, OCTET_PPP);
    private static final int[] SHAPE_BY_STATE = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    public static EnumShape getStairsShape(APlayer player, WrappedBlock originalStairs) {

        BlockFace facing = BlockFace.valueOf(WrappedEnumDirection.fromType1(5 - (originalStairs.getData() & 3))
                .name());
        Optional<WrappedBlock> offsetOne = BlockUtils.getRelative(player, originalStairs.getLocation(), facing);

        if(!offsetOne.isPresent()) return EnumShape.STRAIGHT;

        if (Materials.checkFlag(offsetOne.get().getType(), Materials.STAIRS)
                && (originalStairs.getData() & 4) == (offsetOne.get().getData() & 4)) {
            BlockFace enumfacing1 = BlockFace.valueOf(WrappedEnumDirection.fromType1(5 - (offsetOne.get().getData() & 3))
                    .name());

            if (isDifferentAxis(facing, enumfacing1) && canTakeShape(player, originalStairs, enumfacing1.getOppositeFace().getModX(), enumfacing1.getOppositeFace().getModY(), enumfacing1.getOppositeFace().getModZ())) {
                if (enumfacing1 == rotateYCCW(facing)) {
                    return EnumShape.OUTER_LEFT;
                }

                return EnumShape.OUTER_RIGHT;
            }
        }

        Optional<WrappedBlock> offsetTwo = BlockUtils.getRelative(player, originalStairs.getLocation(),
                facing.getOppositeFace().getModX(),
                facing.getOppositeFace().getModY(), facing.getOppositeFace().getModZ());

        if(!offsetTwo.isPresent()) return EnumShape.STRAIGHT;

        if (Materials.checkFlag(offsetTwo.get().getType(), Materials.STAIRS)
                && (originalStairs.getData() & 4) == (offsetTwo.get().getData() & 4)) {
            BlockFace enumfacing2 = BlockFace.valueOf(WrappedEnumDirection.fromType1(5 - (offsetTwo.get().getData() & 3))
                    .name());

            if (isDifferentAxis(facing, enumfacing2) && canTakeShape(player, originalStairs, enumfacing2.getModX(), enumfacing2.getModY(), enumfacing2.getModZ())) {
                if (enumfacing2 == rotateYCCW(facing)) {
                    return EnumShape.INNER_LEFT;
                }

                return EnumShape.INNER_RIGHT;
            }
        }

        return EnumShape.STRAIGHT;
    }

    private static boolean canTakeShape(APlayer player, WrappedBlock stairOne, int ax, int ay, int az) {
        Optional<WrappedBlock> otherStair = BlockUtils.getRelative(player, stairOne.getLocation(), ax, ay, az);
        return !otherStair.isPresent() || !Materials.checkFlag(otherStair.get().getType(), Materials.STAIRS) ||
                (stairOne.getData() & 3) != (otherStair.get().getData() & 3) ||
                        (stairOne.getData() & 4) != (otherStair.get().getData() & 4);
    }

    private static boolean isDifferentAxis(BlockFace faceOne, BlockFace faceTwo) {
        return faceOne.getOppositeFace() != faceTwo && faceOne != faceTwo;
    }

    private static BlockFace rotateYCCW(BlockFace face) {
        switch (face) {
            default:
            case NORTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
        }
    }

    private static CollisionBox[] makeShapes(CollisionBox p_199779_0_, CollisionBox p_199779_1_, CollisionBox p_199779_2_, CollisionBox p_199779_3_, CollisionBox p_199779_4_) {
        return IntStream.range(0, 16).mapToObj((p_199780_5_) -> makeStairShape(p_199780_5_, p_199779_0_, p_199779_1_, p_199779_2_, p_199779_3_, p_199779_4_)).toArray(CollisionBox[]::new);
    }

    private static CollisionBox makeStairShape(int p_199781_0_, CollisionBox p_199781_1_, CollisionBox p_199781_2_, CollisionBox p_199781_3_, CollisionBox p_199781_4_, CollisionBox p_199781_5_) {
        ComplexCollisionBox voxelshape = new ComplexCollisionBox(p_199781_1_);
        if ((p_199781_0_ & 1) != 0) {
            voxelshape.add(p_199781_2_);
        }

        if ((p_199781_0_ & 2) != 0) {
            voxelshape.add(p_199781_3_);
        }

        if ((p_199781_0_ & 4) != 0) {
            voxelshape.add(p_199781_4_);
        }

        if ((p_199781_0_ & 8) != 0) {
            voxelshape.add(p_199781_5_);
        }

        return voxelshape;
    }

    @Override
    public CollisionBox fetch(ProtocolVersion version, APlayer player, WrappedBlock block) {
        int shapeOrdinal;
        // If server is 1.13+ and client is also 1.13+, we can read the block's data directly
        EnumShape shape = getStairsShape(player, block);
        shapeOrdinal = shape.ordinal();
        return ((block.getData() & 4) > 0 ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_BY_STATE[getShapeIndex(block, shapeOrdinal)]].copy();
    }

    private int getShapeIndex(WrappedBlock state, int shapeOrdinal) {
        return shapeOrdinal * 4 + directionToValue(BlockFace.valueOf(WrappedEnumDirection
                .fromType1(5 - (state.getData() & 3))
                .name()));
    }

    private int directionToValue(BlockFace face) {
        switch (face) {
            default:
            case UP:
            case DOWN:
                return -1;
            case NORTH:
                return 2;
            case SOUTH:
                return 0;
            case WEST:
                return 1;
            case EAST:
                return 3;
        }
    }

    enum EnumShape {
        STRAIGHT,
        INNER_LEFT,
        INNER_RIGHT,
        OUTER_LEFT,
        OUTER_RIGHT
    }
}
