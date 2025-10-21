package dev.brighten.ac.handler.block;

import dev.brighten.ac.handler.entity.TrackedEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.*;

@Getter
public class World {
    private final UUID uuid;
    private final String name;
    private final Long2ObjectOpenHashMap<BlockUpdateHandler.KColumn> chunks = new Long2ObjectOpenHashMap<>(1000);
    private final Map<Integer, TrackedEntity> trackedEntities = new Int2ObjectOpenHashMap<>();

    public World(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
