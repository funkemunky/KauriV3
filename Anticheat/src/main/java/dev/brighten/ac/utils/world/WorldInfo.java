package dev.brighten.ac.utils.world;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.brighten.ac.Anticheat;
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
    private final World world;

    private final Cache<Integer, Entity> entityCache;

    public WorldInfo(World world) {
        this.worldId = world.getUID();
        this.world = world;
        entityCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        Anticheat.INSTANCE.getRunUtils().task(() -> {
            for (Entity entity : world.getEntities()) {
                entityCache.put(entity.getEntityId(), entity);
            }
        });
    }

    public synchronized Optional<Entity> getEntity(int id) {
        try {
            return Optional.of(entityCache.get(id, () -> {
                if(Bukkit.isPrimaryThread()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity.getEntityId() == id) {
                            return entity;
                        }
                    }
                } else {
                    Anticheat.INSTANCE.getRunUtils().task(() -> {
                        for (Entity entity : world.getEntities()) {
                            if (entity.getEntityId() == id) {
                                entityCache.put(id, entity);
                            }
                        }
                    });
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
