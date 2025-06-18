package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.brighten.ac.utils.math.IntVector;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

@Getter
@AllArgsConstructor
public class WrappedBlock {
    private IntVector location;
    private Material type;
    private WrappedBlockState blockState;

    public MaterialData getMaterialData() {
        return SpigotConversionUtil.toBukkitMaterialData(blockState);
    }
}
