package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Optional;

@SuppressWarnings("Duplicates")
public class DynamicWall implements CollisionFactory {

    private static final double width = 0.25;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

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
        Optional<WrappedBlock> targetBlock = BlockUtils.getRelative(player, fenceBlock.getLocation(), direction, 1);

        if(!targetBlock.isPresent()) return false;

        Material target = targetBlock.get().getType();

        if (!isWall(target)&&DynamicFence.isBlacklisted(target))
            return false;

        if(Materials.checkFlag(target, Materials.STAIRS)) {
            if (v.isBelow(ProtocolVersion.V1_12)) return false;

            return dir(fenceBlock.getData()).getOppositeFace() == direction;
        } else return isWall(target) || (target.isSolid() && !target.isTransparent());
    }

    private static boolean isWall(Material m) {
        return Materials.checkFlag(m, Materials.WALL);
    }

    private static BlockFace dir(byte data) {
        switch(data & 3) {
            case 0:
            default:
                return BlockFace.EAST;
            case 1:
                return BlockFace.WEST;
            case 2:
                return BlockFace.SOUTH;
            case 3:
                return BlockFace.NORTH;
        }
    }
}
