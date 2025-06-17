package dev.brighten.ac.utils.world;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import dev.brighten.ac.Anticheat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    public void shutdown() {
        entityCache.invalidateAll();
    }
}
