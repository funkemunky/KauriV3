package dev.brighten.ac.utils.world;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorldInfo {
    @Getter
    private final UUID worldId;
    private World world;

    private final Cache<Integer, Entity> entityCache;

    public WorldInfo(World world) {
        this.worldId = world.getUID();
        this.world = world;
        entityCache = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public synchronized Optional<Entity> getEntity(int id) {
        try {
            return Optional.of(entityCache.get(id, () -> {
                for (Entity entity : Bukkit.getWorld(worldId).getEntities()) {
                    if (entity.getEntityId() == id) {
                        return entity;
                    }
                }
                throw new RuntimeException("Entity " + id + " not found");
            }));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public void shutdown() {
        entityCache.invalidateAll();
    }
}
