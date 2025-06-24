package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.NoCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

public class TrapDoorHandler implements CollisionFactory {
    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock block) {
        BlockFace facing = block.getBlockState().getFacing();
        double var2 = 0.1875;

        if (block.getBlockState().isOpen()) {
            return switch (facing) {
                case SOUTH -> new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, var2);
                case EAST -> new SimpleCollisionBox(0.0, 0.0, 0.0, var2, 1.0, 1.0);
                case WEST -> new SimpleCollisionBox(1.0 - var2, 0.0, 0.0, 1.0, 1.0, 1.0);
                case NORTH -> new SimpleCollisionBox(0.0, 0.0, 1.0 - var2, 1.0, 1.0, 1.0);
                default -> NoCollisionBox.INSTANCE;
            };
        } else {
            if (block.getBlockState().getHalf() != Half.BOTTOM) {
                return new SimpleCollisionBox(0.0, 1.0 - var2, 0.0, 1.0, 1.0, 1.0);
            } else {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, var2, 1.0);
            }
        }
    }
}
