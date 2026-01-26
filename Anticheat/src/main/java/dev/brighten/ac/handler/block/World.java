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
import dev.brighten.ac.utils.LongHash;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.Map;
import java.util.Optional;

@Getter
public class World {
    private final String name;
    private final Long2ObjectOpenHashMap<BlockUpdateHandler.KColumn> chunks = new Long2ObjectOpenHashMap<>(1000);
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
        synchronized (chunks) {
            long hash = LongHash.toLong(x >> 4, z >> 4);
            BlockUpdateHandler.KColumn chunk = chunks.get(hash);

            // If the chunk is null, create a new one
            if(chunk == null) {
                return chunks.put(hash, new BlockUpdateHandler.KColumn(x, z, new BaseChunk[maxHeight / 16]));
            }

            return chunk;
        }
    }

    void updateChunk(Column chunk) {
        synchronized (chunks) {
            BlockUpdateHandler.KColumn column = new BlockUpdateHandler.KColumn(chunk.getX(), chunk.getZ(), chunk.getChunks());
            chunks.put(LongHash.toLong(column.x(), column.z()), column);
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

        BaseChunk chunk = col.chunks().length - 1 < (y >> 4) ? null : col.chunks()[y >> 4];

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
