package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import javax.annotation.Nullable;

public class PacketEventsUtil {

    public static @Nullable EntityType convertBukkitEntityType(org.bukkit.entity.EntityType bukkitEntity) {
        return switch (bukkitEntity) {
            case PLAYER -> EntityTypes.PLAYER;
            case ZOMBIE -> EntityTypes.ZOMBIE;
            case SKELETON -> EntityTypes.SKELETON;
            case CREEPER -> EntityTypes.CREEPER;
            case SPIDER -> EntityTypes.SPIDER;
            case ENDERMAN -> EntityTypes.ENDERMAN;
            case COW -> EntityTypes.COW;
            case PIG -> EntityTypes.PIG;
            case SHEEP -> EntityTypes.SHEEP;
            case CHICKEN -> EntityTypes.CHICKEN;
            case WOLF -> EntityTypes.WOLF;
            case OCELOT -> EntityTypes.OCELOT;
            case HORSE -> EntityTypes.HORSE;
            case VILLAGER -> EntityTypes.VILLAGER;
            case IRON_GOLEM -> EntityTypes.IRON_GOLEM;
            case SNOWMAN -> EntityTypes.SNOW_GOLEM;
            case BAT -> EntityTypes.BAT;
            case RABBIT -> EntityTypes.RABBIT;
            case SNOWBALL -> EntityTypes.SNOWBALL;
            case ARROW -> EntityTypes.ARROW;
            case FIREBALL -> EntityTypes.FIREBALL;
            case ENDER_PEARL -> EntityTypes.ENDER_PEARL;
            case EGG -> EntityTypes.EGG;
            case EXPERIENCE_ORB -> EntityTypes.EXPERIENCE_ORB;
            case ITEM_FRAME -> EntityTypes.ITEM_FRAME;
            case PAINTING -> EntityTypes.PAINTING;
            case MINECART -> EntityTypes.MINECART;
            case MINECART_CHEST -> EntityTypes.CHEST_MINECART;
            case MINECART_FURNACE -> EntityTypes.FURNACE_MINECART;
            case MINECART_TNT -> EntityTypes.TNT_MINECART;
            case MINECART_HOPPER -> EntityTypes.HOPPER_MINECART;
            case MINECART_COMMAND -> EntityTypes.COMMAND_BLOCK_MINECART;
            case LEASH_HITCH -> EntityTypes.LEASH_KNOT;
            case ENDER_DRAGON -> EntityTypes.ENDER_DRAGON;
            case WITHER -> EntityTypes.WITHER;
            case FALLING_BLOCK -> EntityTypes.FALLING_BLOCK;
            case PRIMED_TNT -> EntityTypes.PRIMED_TNT;
            case ENDER_CRYSTAL -> EntityTypes.END_CRYSTAL;
            case FISHING_HOOK -> EntityTypes.FISHING_BOBBER;
            default -> null;
        };
    }
}
