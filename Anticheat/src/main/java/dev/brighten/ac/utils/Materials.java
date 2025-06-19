package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.types.NoCollisionBox;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class Materials {
    private static final Map<StateType, Integer> MATERIAL_FLAGS =  new HashMap<>();

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
        int i = 0;
        for (StateType mat : StateTypes.values()) {
            
            int flag = MATERIAL_FLAGS.getOrDefault(mat, 0);

            //We use the one in BlockUtils also since we can't trust Material to include everything.
            if (mat.isSolid() || mat.getName().contains("COMPARATOR") || mat.getName().contains("DIODE")) {
                flag |= SOLID;
            }

            if(!(BlockData.getData(mat).getDefaultBox() instanceof NoCollisionBox)) {
                flag |= COLLIDABLE;
            }

            if (mat.getName().endsWith("_STAIRS")) {
                flag |= STAIRS;
            }

            if (mat.getName().contains("SLAB") || mat.getName().contains("STEP")) {
                flag |= SLABS;
            }

            if(mat.getName().contains("SKULL"))
                flag |= SOLID;

            if(mat.getName().contains("STATIONARY") || mat.getName().contains("LAVA") || mat.getName().contains("WATER")) {
                if(mat.getName().contains("LAVA")) {
                    flag |= LIQUID | LAVA;
                } else flag |= LIQUID | WATER;
            }

            if (mat.getName().contains("FENCE")) {
                if(!mat.getName().contains("GATE")) flag |= FENCE;
            }
            if(mat.getName().contains("WALL")) flag |= WALL;
            if(mat.getName().contains("BED") && !mat.getName().contains("ROCK")) flag  |= SLABS;
            if(mat.getName().contains("ICE")) flag |= ICE;
            if(mat.getName().contains("CARPET")) flag |= SOLID;
            //Signs get set as collidable when they shouldn't
            if(mat.getName().contains("SIGN")) flag = 0;

            MATERIAL_FLAGS.put(mat, flag);
            i++;
        }

        // fix some types where isSolid() returns the wrong value
        MATERIAL_FLAGS.put(StateTypes.REPEATER, (MATERIAL_FLAGS.get(StateTypes.REPEATER)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.SNOW, (MATERIAL_FLAGS.get(StateTypes.SNOW)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.SNOW_BLOCK, (MATERIAL_FLAGS.get(StateTypes.SNOW_BLOCK)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.ANVIL, (MATERIAL_FLAGS.get(StateTypes.COBBLESTONE_WALL)) | WALL);
        MATERIAL_FLAGS.put(StateTypes.CHIPPED_ANVIL, (MATERIAL_FLAGS.get(StateTypes.COBBLESTONE_WALL)) | WALL);
        MATERIAL_FLAGS.put(StateTypes.DAMAGED_ANVIL, (MATERIAL_FLAGS.get(StateTypes.COBBLESTONE_WALL)) | WALL);
        MATERIAL_FLAGS.put(StateTypes.LILY_PAD, (MATERIAL_FLAGS.get(StateTypes.LILY_PAD)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.SLIME_BLOCK, (MATERIAL_FLAGS.get(StateTypes.SLIME_BLOCK)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.SCAFFOLDING, (MATERIAL_FLAGS.get(StateTypes.SCAFFOLDING)) | SOLID);
        MATERIAL_FLAGS.put(StateTypes.LADDER, (MATERIAL_FLAGS.get(StateTypes.LADDER)) | LADDER | SOLID);
        MATERIAL_FLAGS.put(StateTypes.VINE, (MATERIAL_FLAGS.get(StateTypes.VINE)) | LADDER | SOLID);
    }

    public static int getBitmask(StateType material) {
        return MATERIAL_FLAGS.getOrDefault(material, 0);
    }

    private Materials() {

    }

    public static boolean checkFlag(StateType material, int flag) {
        return (MATERIAL_FLAGS.get(material) & flag) == flag;
    }

    public static boolean isUsable(Material material) {
        String nameLower = material.name().toLowerCase();
        return material.isEdible()
                || nameLower.contains("bow")
                || nameLower.contains("sword")
                || nameLower.contains("trident");
    }

}