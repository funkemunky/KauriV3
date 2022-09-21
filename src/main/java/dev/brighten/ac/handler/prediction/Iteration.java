package dev.brighten.ac.handler.prediction;

import lombok.AllArgsConstructor;
import org.bukkit.Material;

@AllArgsConstructor
public class Iteration {
    public Material underMaterial, lastUnderMaterial;
    public int f, s, fastMath;
    public boolean sprinting, attack, using, sneaking, jumped;
}
