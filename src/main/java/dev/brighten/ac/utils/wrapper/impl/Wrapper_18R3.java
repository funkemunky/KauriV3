package dev.brighten.ac.utils.wrapper.impl;

import dev.brighten.ac.utils.wrapper.Wrapper;
import net.minecraft.server.v1_8_R3.BlockPosition;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;

public class Wrapper_18R3 extends Wrapper {
    @Override
    public float getFriction(Material material) {
        return CraftMagicNumbers.getBlock(material).frictionFactor;
    }

    @Override
    public Material getType(World world, double x, double y, double z) {
        BlockPosition blockPos = new BlockPosition(x, y, z);
        return CraftMagicNumbers.getMaterial(((CraftWorld)world).getHandle().getType(blockPos).getBlock());
    }
}
