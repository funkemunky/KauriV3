package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_7.Chunk_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_8.Chunk_v1_8;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.KLocation;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.Map;
import java.util.Optional;

@Getter
public class World {
    private final String name;
    // Array dimensions: 64×64 covers a render distance of up to 32 chunks without
    // collision. Index = (chunkX & MASK) * SIZE + (chunkZ & MASK); stored KColumn
    // x/z are chunk coordinates and are validated on every lookup.
    private static final int CHUNK_ARRAY_BITS = 6;                         // 2^6 = 64 per axis
    private static final int CHUNK_ARRAY_SIZE = 1 << CHUNK_ARRAY_BITS;    // 64
    private static final int CHUNK_ARRAY_MASK = CHUNK_ARRAY_SIZE - 1;     // 63
    private final BlockUpdateHandler.KColumn[] chunks = new BlockUpdateHandler.KColumn[CHUNK_ARRAY_SIZE * CHUNK_ARRAY_SIZE];
    private final Map<Integer, TrackedEntity> trackedEntities = new Int2ObjectOpenHashMap<>();

    public World(String name) {
        this.name = name;
    }

    private int minHeight = 0, maxHeight = 256;

    public void setMinHeight(DimensionType type) {
        minHeight = type.getMinY();
        maxHeight = minHeight + type.getHeight();
    }

    /**
     * Keep track of block diggings since the Bukkit API will be a bit behind
     * @param x x coordinate
     * @param z z coordinate
     * @return the chunk at the specified coordinates
     */
    public BlockUpdateHandler.KColumn getChunk(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        synchronized (chunks) {
            int index = chunkIndex(chunkX, chunkZ);
            BlockUpdateHandler.KColumn chunk = chunks[index];

            // Validate that the stored entry belongs to this chunk position
            if (chunk == null || chunk.x() != chunkX || chunk.z() != chunkZ) {
                chunk = new BlockUpdateHandler.KColumn(chunkX, chunkZ, new BaseChunk[maxHeight / 16]);
                chunks[index] = chunk;
            }

            return chunk;
        }
    }

    private static int chunkIndex(int chunkX, int chunkZ) {
        return ((chunkX & CHUNK_ARRAY_MASK) << CHUNK_ARRAY_BITS) | (chunkZ & CHUNK_ARRAY_MASK);
    }

    void updateChunk(Column chunk) {
        synchronized (chunks) {
            int index = chunkIndex(chunk.getX(), chunk.getZ());
            chunks[index] = new BlockUpdateHandler.KColumn(chunk.getX(), chunk.getZ(), chunk.getChunks());
        }
    }

    private static BaseChunk create() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
            return new Chunk_v1_18();
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return new Chunk_v1_9(0, PaletteType.BIOME.create().paletteType.create());
        } else if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)) {
            return new Chunk_v1_8(false);
        }
        return new Chunk_v1_7(false, false);
    }


    /**
     * Get a block at the specified coordinates
     * @param location the location of the block
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(KLocation location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Get a block at the specified coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(int x, int y, int z) {
        BlockUpdateHandler.KColumn col = getChunk(x, z);

        y -= minHeight;

        if(col == null) {
            return new WrappedBlock(new Vector3i(x, y, z),
                    StateTypes.AIR,
                    BlockUpdateHandler.airBlockState);
        }

        BaseChunk chunk = col.chunks().length - 1 < (y >> 4) ? null : col.chunks()[Math.max(0, (y >> 4))];

        if(chunk == null) {
            //Get Bukkit Block
            return new WrappedBlock(new Vector3i(x, y, z),
                    StateTypes.AIR,
                    BlockUpdateHandler.airBlockState);
        }

        WrappedBlockState state = chunk.get(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),x & 15, y & 15, z & 15);

        return new WrappedBlock(new Vector3i(x, y, z),
                state.getType(),
                state);
    }

    void updateBlock(int x, int y, int z, WrappedBlockState blockState) {
        BlockUpdateHandler.KColumn col = getChunk(x, z);

        int offset = y - minHeight;

        if(offset < 0 || (offset >> 4) > col.chunks().length) {
            return;
        }

        BaseChunk chunk = col.chunks()[offset >> 4];

        if(chunk == null) {
            chunk = create();
            col.chunks()[offset >> 4] = chunk;

            chunk.set(null, 0, 0, 0, 0);
        }

        chunk.set(x & 15, offset & 15, z & 15,
                blockState);
    }

    /**
     * Get a block at the specified coordinates
     * @param vec the coordinates
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(Vector3i vec) {
        return getBlock(vec.getX(), vec.getY(), vec.getZ());
    }

    public WrappedBlock getBlock(Vector3d vec) {
        return getBlock(
                MathHelper.floor_double(vec.getX()),
                MathHelper.floor_double(vec.getY()),
                MathHelper.floor_double(vec.getZ())
        );
    }

    public Optional<TrackedEntity> getTrackedEntity(int entityId) {
        TrackedEntity trackedEntity = trackedEntities.get(entityId);

        if(trackedEntity == null) {
            return Optional.empty();
        }

        return Optional.of(trackedEntity);
    }
}
