package dev.brighten.ac.utils.wrapper.impl;

import dev.brighten.ac.utils.wrapper.Wrapper;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;

public class Wrapper_18R3 extends Wrapper {
    @Override
    public float getFriction(Material material) {
        return CraftMagicNumbers.getBlock(material).frictionFactor;
    }
}
