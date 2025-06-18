package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.ComplexCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;

import java.util.Optional;

public class DynamicFence implements CollisionFactory {

    private static final double width = 0.125;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock b) {
        ComplexCollisionBox box = new ComplexCollisionBox(new SimpleCollisionBox(min, 0, min, max, 1.5, max));
        boolean east =  fenceConnects(version, player, b, BlockFace.EAST);
        boolean north = fenceConnects(version, player, b, BlockFace.NORTH);
        boolean south = fenceConnects(version, player, b, BlockFace.SOUTH);
        boolean west =  fenceConnects(version, player, b, BlockFace.WEST);
        if (east) box.add(new SimpleCollisionBox(max, 0, min, 1, 1.5, max));
        if (west) box.add(new SimpleCollisionBox(0, 0, min, max, 1.5, max));
        if (north) box.add(new SimpleCollisionBox(min, 0, 0, max, 1.5, min));
        if (south) box.add(new SimpleCollisionBox(min, 0, max, max, 1.5, 1));
        return box;
    }

    static boolean isBlacklisted(Material m) {
        XMaterial material = BlockUtils.getXMaterial(m);
        switch(material) {
            case BEACON:
            case STICK:
            case MELON:
            case DAYLIGHT_DETECTOR:
            case BARRIER:
                return true;
            default:
                return !Materials.checkFlag(m, Materials.SOLID)
                        || Materials.checkFlag(m, Materials.WALL)
                        || Materials.checkFlag(m, Materials.FENCE)
                        || m.name().contains("DAYLIGHT");
        }
    }

    private static boolean fenceConnects(ClientVersion v, APlayer player, WrappedBlock fenceBlock, BlockFace direction) {
        Optional<WrappedBlock> targetBlock = BlockUtils.getRelative(player, fenceBlock.getLocation(), direction, 1);

        if(targetBlock.isEmpty()) return false;

        Material target = targetBlock.get().getType();
        Material fence = fenceBlock.getType();

        MaterialData fenceData = SpigotConversionUtil.toBukkitMaterialData(fenceBlock.getBlockState());

        if (!isFence(target)&&isBlacklisted(target))
            return false;

        if(Materials.checkFlag(target, Materials.STAIRS)) {
            if (v.isOlderThan(ClientVersion.V_1_12)) return false;

            return dir(fenceData.getData()).getOppositeFace() == direction;
        } else if(target.name().contains("GATE")) {

            BlockFace f1 = dir(SpigotConversionUtil.toBukkitMaterialData(targetBlock.get().getBlockState()).getData());
            BlockFace f2 = f1.getOppositeFace();
            return direction == f1 || direction == f2;
        } else {
            if (fence == target) return true;
            if (isFence(target))
                return !fence.name().contains("NETHER") && !target.name().contains("NETHER");
            else return isFence(target) || (target.isSolid() && !target.isTransparent());
        }
    }

    private static boolean isFence(Material material) {
        return Materials.checkFlag(material, Materials.FENCE) && material.name().contains("FENCE");
    }

    private static BlockFace dir(byte data) {
        return switch(data & 3) {
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.SOUTH;
            case 3 -> BlockFace.NORTH;
            default ->  BlockFace.EAST;
        };
    }

}
