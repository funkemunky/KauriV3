package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.wrapper.Wrapper;
import org.bukkit.Material;

public class Materials {
    private static final int[] MATERIAL_FLAGS = new int[Material.values().length];

    public static final int SOLID  = 0b00000000000000000000000000001;
    public static final int LADDER = 0b00000000000000000000000000010;
    public static final int WALL   = 0b00000000000000000000000000100;
    public static final int STAIRS = 0b00000000000000000000000001000;
    public static final int SLABS  = 0b00000000000000000000000010000;
    public static final int WATER  = 0b00000000000000000000000100000;
    public static final int LAVA   = 0b00000000000000000000001000000;
    public static final int LIQUID = 0b00000000000000000000010000000;
    public static final int ICE    = 0b00000000000000000000100000000;
    public static final int FENCE  = 0b00000000000000000001000000000;
    public static final int COLLIDABLE = 0b00000000000000000010000000000;

    static {
        for (int i = 0; i < MATERIAL_FLAGS.length; i++) {
            Material mat = Material.values()[i];

            //We use the one in BlockUtils also since we can't trust Material to include everything.
            if (mat.isSolid() || mat.name().contains("COMPARATOR") || mat.name().contains("DIODE")) {
                MATERIAL_FLAGS[i] |= SOLID;
            }

            if(Wrapper.getInstance().isCollidable(mat)) {
                MATERIAL_FLAGS[i] |= COLLIDABLE;
            }

            if (mat.name().endsWith("_STAIRS")) {
                MATERIAL_FLAGS[i] |= STAIRS;
            }

            if (mat.name().contains("SLAB") || mat.name().contains("STEP")) {
                MATERIAL_FLAGS[i] |= SLABS;
            }

            if(mat.name().contains("SKULL"))
                MATERIAL_FLAGS[i] |= SOLID;

            if(mat.name().contains("STATIONARY") || mat.name().contains("LAVA") || mat.name().contains("WATER")) {
                if(mat.name().contains("LAVA")) {
                    MATERIAL_FLAGS[i] |= LIQUID | LAVA;
                } else MATERIAL_FLAGS[i] |= LIQUID | WATER;
            }

            if (mat.name().contains("FENCE")) {
                if(!mat.name().contains("GATE")) MATERIAL_FLAGS[mat.ordinal()] |= FENCE;
            }
            if(mat.name().contains("WALL")) MATERIAL_FLAGS[mat.ordinal()] |= WALL;
            if(mat.name().contains("BED") && !mat.name().contains("ROCK")) MATERIAL_FLAGS[mat.ordinal()]  |= SLABS;
            if(mat.name().contains("ICE")) MATERIAL_FLAGS[mat.ordinal()] |= ICE;
            if(mat.name().contains("CARPET")) MATERIAL_FLAGS[mat.ordinal()] |= SOLID;
            //Signs get set as collidable when they shouldn't
            if(mat.name().contains("SIGN")) MATERIAL_FLAGS[mat.ordinal()] = 0;
        }

        // fix some types where isSolid() returns the wrong value
        MATERIAL_FLAGS[XMaterial.REPEATER.parseMaterial().ordinal()] |= SOLID;
        MATERIAL_FLAGS[XMaterial.SNOW.parseMaterial().ordinal()] |= SOLID;
        MATERIAL_FLAGS[XMaterial.ANVIL.parseMaterial().ordinal()] |= SOLID;
        MATERIAL_FLAGS[XMaterial.LILY_PAD.parseMaterial().ordinal()] |= SOLID;

        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)) {
            MATERIAL_FLAGS[XMaterial.SLIME_BLOCK.parseMaterial().ordinal()] |= SOLID;

            if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
                MATERIAL_FLAGS[XMaterial.SCAFFOLDING.parseMaterial().ordinal()] |= SOLID;
            }
        }

        // ladders
        MATERIAL_FLAGS[XMaterial.LADDER.parseMaterial().ordinal()] |= LADDER | SOLID;
        MATERIAL_FLAGS[XMaterial.VINE.parseMaterial().ordinal()] |= LADDER | SOLID;
    }

    public static int getBitmask(Material material) {
        return MATERIAL_FLAGS[material.ordinal()];
    }

    private Materials() {

    }

    public static boolean checkFlag(Material material, int flag) {
        return (MATERIAL_FLAGS[material.ordinal()] & flag) == flag;
    }

    public static boolean isUsable(Material material) {
        String nameLower = material.name().toLowerCase();
        return material.isEdible()
                || nameLower.contains("bow")
                || nameLower.contains("sword")
                || nameLower.contains("trident");
    }

}