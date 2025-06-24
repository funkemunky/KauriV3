package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.ComplexCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.Optional;

@SuppressWarnings("Duplicates")
public class DynamicPane implements CollisionFactory {

    private static final double width = 0.0625;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock b) {
        ComplexCollisionBox box = new ComplexCollisionBox(new SimpleCollisionBox(min, 0, min, max, 1, max));
        boolean east =  fenceConnects(version, player, b, BlockFace.EAST);
        boolean north = fenceConnects(version, player, b, BlockFace.NORTH);
        boolean south = fenceConnects(version, player, b, BlockFace.SOUTH);
        boolean west =  fenceConnects(version, player, b, BlockFace.WEST);

        if (version.isOlderThan(ClientVersion.V_1_9) && !(east||north||south||west)) {
            east = true;
            west = true;
            north = true;
            south = true;
        }

        if (east) box.add(new SimpleCollisionBox(max, 0, min, 1, 1, max));
        if (west) box.add(new SimpleCollisionBox(0, 0, min, max, 1, max));
        if (north) box.add(new SimpleCollisionBox(min, 0, 0, max, 1, min));
        if (south) box.add(new SimpleCollisionBox(min, 0, max, max, 1, 1));
        return box;
    }


    private static boolean fenceConnects(ClientVersion v, APlayer player, WrappedBlock fenceBlock, BlockFace direction) {
        Optional<WrappedBlock> targetBlock = BlockUtils.getRelative(player, fenceBlock.getLocation(), direction, 1);

        if(targetBlock.isEmpty()) return false;
        StateType target = targetBlock.get().getType();

        if (!isPane(target)&&DynamicFence.isBlacklisted(target))
            return false;

        if(Materials.checkFlag(target, Materials.STAIRS)) {
            if (v.isOlderThan(ClientVersion.V_1_12)) return false;

            return fenceBlock.getBlockState().getFacing().getOppositeFace() == direction;
        }  else return isPane(target) || (target.isSolid() && !target.isBlocking());
    }

    private static boolean isPane(StateType m) {
        return m == StateTypes.IRON_BARS || m.getName().toUpperCase().contains("PANE")
                || m.getName().toUpperCase().contains("THIN");
    }

}
