package dev.brighten.ac.utils.world;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.world.types.NoCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class EntityData {
    private static final Map<EntityType, CollisionBox> entityBounds = new HashMap<>();
    public static CollisionBox bounds(EntityType entity) {
        return entityBounds.computeIfAbsent(entity, type -> NoCollisionBox.INSTANCE).copy();
    }

    public static CollisionBox getEntityBox(Location location, TrackedEntity entity) {
        return bounds(entity.getEntityType()).offset(location.getX(), location.getY(), location.getZ());
    }

    public static CollisionBox getEntityBox(Location location, EntityType entity) {
        return bounds(entity).offset(location.getX(), location.getY(), location.getZ());
    }

    @SuppressWarnings("deprecation")
    public static CollisionBox getEntityBox(Entity entity) {
        return getEntityBox(entity.getLocation(),
                EntityTypes.getById(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                        entity.getType().getTypeId()));
    }

    static {
        entityBounds.put(EntityTypes.WOLF, new SimpleCollisionBox(new Vector(), 0.62f, .8f));
        entityBounds.put(EntityTypes.SHEEP, new SimpleCollisionBox(new Vector(), 0.9f, 1.3f));
        entityBounds.put(EntityTypes.COW, new SimpleCollisionBox(new Vector(), 0.9f, 1.3f));
        entityBounds.put(EntityTypes.PIG, new SimpleCollisionBox(new Vector(), 0.9f, 0.9f));
        entityBounds.put(EntityTypes.MOOSHROOM, new SimpleCollisionBox(new Vector(), 0.9f, 1.3f));
        entityBounds.put(EntityTypes.WITCH, new SimpleCollisionBox(new Vector(), 0.62f, 1.95f));
        entityBounds.put(EntityTypes.BLAZE, new SimpleCollisionBox(new Vector(), 0.62f, 1.8f));
        entityBounds.put(EntityTypes.PLAYER, new SimpleCollisionBox(new Vector(), 0.6f, 1.8f));
        entityBounds.put(EntityTypes.VILLAGER, new SimpleCollisionBox(new Vector(), 0.6f, 1.8f));
        entityBounds.put(EntityTypes.CREEPER, new SimpleCollisionBox(new Vector(), 0.6f, 1.8f));
        entityBounds.put(EntityTypes.GIANT, new SimpleCollisionBox(new Vector(), 3.6f, 10.8f));
        entityBounds.put(EntityTypes.SKELETON, new SimpleCollisionBox(new Vector(), 0.6f, 1.95F));
        entityBounds.put(EntityTypes.ZOMBIE, new SimpleCollisionBox(new Vector(), 0.6f, 1.95F));
        entityBounds.put(EntityTypes.SNOW_GOLEM, new SimpleCollisionBox(new Vector(), 0.7f, 1.9f));
        entityBounds.put(EntityTypes.HORSE, new SimpleCollisionBox(new Vector(), 1.4f, 1.6f));
        entityBounds.put(EntityTypes.ENDER_DRAGON, new SimpleCollisionBox(new Vector(), 3f, 1.5f));
        entityBounds.put(EntityTypes.ENDERMAN, new SimpleCollisionBox(new Vector(), 0.6f, 2.9f));
        entityBounds.put(EntityTypes.CHICKEN, new SimpleCollisionBox(new Vector(), 0.4f, 0.7f));
        entityBounds.put(EntityTypes.OCELOT, new SimpleCollisionBox(new Vector(), 0.62f, 0.7f));
        entityBounds.put(EntityTypes.SPIDER, new SimpleCollisionBox(new Vector(), 1.4f, 0.9f));
        entityBounds.put(EntityTypes.WITHER, new SimpleCollisionBox(new Vector(), 0.9f, 3.5f));
        entityBounds.put(EntityTypes.IRON_GOLEM, new SimpleCollisionBox(new Vector(), 1.4f, 2.9f));
        entityBounds.put(EntityTypes.GHAST, new SimpleCollisionBox(new Vector(), 4f, 4f));
    }
}
