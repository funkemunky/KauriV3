package dev.brighten.ac.handler.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public class FakeEntityTracker {
    @Getter
    private final Int2ObjectMap<FakeMob> entityMap = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public FakeMob getEntityById(int id) {
        return entityMap.get(id);
    }

    public void trackEntity(FakeMob player) {
        entityMap.put(player.getEntityId(), player);
    }

    public void untrackEntity(FakeMob player) {
        entityMap.remove(player.getEntityId());
    }

    public void despawnAll() {
        entityMap.values().forEach(FakeMob::despawn);
        entityMap.clear();
    }
}
