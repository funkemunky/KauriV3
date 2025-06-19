package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.Optional;

@SuppressWarnings("Duplicates")
public class DynamicWall implements CollisionFactory {

    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock b) {
        boolean var3 = wallConnects(version, player, b, BlockFace.NORTH);
        boolean var4 = wallConnects(version, player, b, BlockFace.SOUTH);
        boolean var5 = wallConnects(version, player, b, BlockFace.WEST);
        boolean var6 = wallConnects(version, player, b, BlockFace.EAST);

        double var7 = 0.25;
        double var8 = 0.75;
        double var9 = 0.25;
        double var10 = 0.75;

        if (var3) {
            var9 = 0.0;
        }

        if (var4) {
            var10 = 1.0;
        }

        if (var5) {
            var7 = 0.0;
        }

        if (var6) {
            var8 = 1.0;
        }

        if (var3 && var4 && !var5 && !var6) {
            var7 = 0.3125;
            var8 = 0.6875;
        } else if (!var3 && !var4 && var5 && var6) {
            var9 = 0.3125;
            var10 = 0.6875;
        }

        return new SimpleCollisionBox(var7, 0.0, var9, var8, 1.5, var10);
    }

    private static boolean wallConnects(ClientVersion v, APlayer player, WrappedBlock fenceBlock, BlockFace direction) {
        Optional<WrappedBlock> targetBlock = player == null ? Optional.empty() : BlockUtils.getRelative(player, fenceBlock.getLocation(), direction, 1);

        if(targetBlock.isEmpty()) return false;

        StateType target = targetBlock.get().getType();

        if (!isWall(target)&&DynamicFence.isBlacklisted(target))
            return false;

        if(Materials.checkFlag(target, Materials.STAIRS)) {
            if (v.isOlderThan(ClientVersion.V_1_12)) return false;

            return fenceBlock.getBlockState().getFacing().getOppositeFace() == direction;
        } else return isWall(target) || (target.isSolid() && !target.isBlocking());
    }

    private static boolean isWall(StateType m) {
        return Materials.checkFlag(m, Materials.WALL);
    }
}
