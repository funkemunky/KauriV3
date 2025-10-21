package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_7.Chunk_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_8.Chunk_v1_8;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkDataBulk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.LongHash;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.types.RayCollision;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class BlockUpdateHandler {
    private final Long2ObjectOpenHashMap<KColumn> chunks = new Long2ObjectOpenHashMap<>(1000);

    private final APlayer player;

    /**
     * Clear all chunks when the player changes worlds
     */
    public void onWorldChange() {
        synchronized (chunks) {
            chunks.clear();
        }

        setMinHeight(player.getDimensionType());
    }

    /**
     * Keep track of block placements since the Bukkit API will be a bit behind
     *
     * @param place wrapped PacketPlayInBlockPlace
     */
    public void onPlace(WrapperPlayClientPlayerBlockPlacement place) {
        player.getInfo().lastBlockUpdate.reset();
        // Could not possibly be a block placement as it's not a block a player is holding.
        IntVector pos = new IntVector(place.getBlockPosition());

        // Some dumbass shit I have to do because Minecraft with Lilypads
        if (place.getItemStack().isPresent()
                && BlockUtils.getXMaterial(place.getItemStack().get().getType()).equals(XMaterial.LILY_PAD)) {
            RayCollision rayCollision = new RayCollision(player.getBukkitPlayer().getEyeLocation().toVector(),
                    player.getBukkitPlayer().getLocation().getDirection());
            WrappedBlock block = rayCollision.getClosestBlockOfType(player, Materials.LIQUID, 5);

            if (block != null) {
                pos = new IntVector(block.getLocation().getX(), block.getLocation().getY() + 1, block.getLocation().getZ());
            } else return;
        } // Not an actual block place, just an interact
        else if (pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
            return;
        } else {
            pos.setX(pos.getX() + place.getFace().getModX());
            pos.setY(pos.getY() + place.getFace().getModY());
            pos.setZ(pos.getZ() + place.getFace().getModZ());
        }

        player.getInfo().getLastPlace().reset();


        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if(place.getItemStack().isEmpty() || place.getItemStack().get().getType().getPlacedType() == null) {
            return;
        }

        updateBlock(x, y, z, WrappedBlockState.getDefaultState(place.getItemStack().get().getType().getPlacedType()));
    }

    /**
     * Keep track of block diggings since the Bukkit API will be a bit behind
     * @param x x coordinate
     * @param z z coordinate
     * @return the chunk at the specified coordinates
     */
    public KColumn getChunk(int x, int z) {
        synchronized (chunks) {
            long hash = LongHash.toLong(x >> 4, z >> 4);
            KColumn chunk = chunks.get(hash);

            // If the chunk is null, create a new one
            if(chunk == null) {
                chunk = getBukkitColumn(player.getBukkitPlayer().getWorld(), x, z);

                if(chunk == null) {
                    return new KColumn(x, z, new BaseChunk[maxHeight / 16]);
                }
                chunks.put(hash, chunk);
            }

            return chunk;
        }
    }

    private KColumn getBukkitColumn(World world, int x, int z) {
        Chunk chunk = BlockUtils.getChunkAsync(world, x >> 4, z >> 4).orElse(null);

        if(chunk == null) {
            // Handle loading on main thread
            Anticheat.INSTANCE.getRunUtils().task(() -> {
                long hash = LongHash.toLong(x >> 4, z >> 4);

                if(!chunks.containsKey(hash)) {
                    chunks.put(hash, getBukkitColumn(world, x, z));
                }
            });
            return null;
        }
        BaseChunk[] levels = new BaseChunk[world.getMaxHeight() / 16];

        for(int i = 0; i < levels.length; i++) {
            levels[i] = create();
        }

        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        for(int blockX = chunkX; blockX < chunkX + 16 ; blockX++) {
            for(int blockZ = chunkZ; blockZ < chunkZ + 16 ; blockZ++) {
                for(int blockY = minHeight ; blockY < world.getMaxHeight() ; blockY++) {
                    Block block = chunk.getBlock(blockX, blockY, blockZ);

                    if(block.getType() == null || block.getType().equals(Material.AIR)) {
                        continue; // Air
                    }

                    BaseChunk baseChunk = levels[blockY >> 4];

                    WrappedBlockState state = SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData());

                    baseChunk.set(blockX & 15, blockY & 15, blockZ & 15, state);
                }
            }
        }

        return new KColumn(x, z, levels);
    }

    private void updateChunk(Column chunk) {
        synchronized (chunks) {
            KColumn column = new KColumn(chunk.getX(), chunk.getZ(), chunk.getChunks());
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
    public WrappedBlock getBlock(Location location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private int minHeight = 0, maxHeight = 256;

    public void setMinHeight(DimensionType type) {
        minHeight = type.getMinY();
        maxHeight = minHeight + type.getHeight();
    }

    private static final WrappedBlockState airBlockState = WrappedBlockState.getByGlobalId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), 0);

    /**
     * Get a block at the specified coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(int x, int y, int z) {
        KColumn col = getChunk(x, z);

        y -= minHeight;

        final int worldY = y;

        // Convert to internal offset relative to minHeight
        y -= minHeight;

        // Fast bounds check for absurd/invalid Y before chunk indexing
        if (y < 0 || y >= (maxHeight - minHeight)) {
            return new WrappedBlock(new IntVector(x, worldY, z),
                    StateTypes.AIR,
                    airBlockState);
        }

        // Also ensure the section index is within the cached column bounds
        final int sectionIndex = y >> 4;
        if (sectionIndex >= col.chunks().length) {
            return new WrappedBlock(new IntVector(x, worldY, z),
                    StateTypes.AIR,
                    airBlockState);
        }

        BaseChunk chunk = col.chunks()[sectionIndex];

        if(chunk == null) {
            //Get Bukkit Block
            Block block = BlockUtils.getBlockAsync(new Location(player.getBukkitPlayer().getWorld(), x, y, z))
                    .orElse(null);

            if(block != null) {
                WrappedBlockState state = SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData());

                if (state.getType() == StateTypes.AIR) {
                    return new WrappedBlock(new IntVector(x, y + minHeight, z),
                            StateTypes.AIR,
                            airBlockState);
                } else {
                    chunk = create();
                    col.chunks()[y >> 4] = chunk;
                    chunk.set(x & 15, y & 15, z & 15, state);
                }
            } else return new WrappedBlock(new IntVector(x, y, z),
                    StateTypes.AIR,
                    airBlockState);
        }

        WrappedBlockState state = chunk.get(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),x & 15, y & 15, z & 15);

        return new WrappedBlock(new IntVector(x, y, z),
                state.getType(),
                state);
    }



    /**
     * Get a block at the specified coordinates
     * @param vec the coordinates
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(IntVector vec) {
        return getBlock(vec.getX(), vec.getY(), vec.getZ());
    }

    public WrappedBlock getBlock(Vector3d vec) {
        return getBlock(
                MathHelper.floor_double(vec.getX()),
                MathHelper.floor_double(vec.getY()),
                MathHelper.floor_double(vec.getZ())
        );
    }

    /**
     * Get a block at the specified coordinates
     * @param block Grabs coordinates from @link{org.bukkit.block.Block}
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(Block block) {
        return getBlock(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Keep track of block breaking since the Bukkit API will be a bit behind.
     *
     * @param dig Wrapped PacketPlayInBlockDig
     */
    public void onDig(WrapperPlayClientPlayerDigging dig) {
        player.getInfo().lastBlockUpdate.reset();
        if (dig.getAction() == DiggingAction.FINISHED_DIGGING) {

            IntVector pos = new IntVector(dig.getBlockPosition());

            if (pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
                return;
            }

            updateBlock(pos.getX(), pos.getY(), pos.getZ(), airBlockState);
        }
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param packet Wrapped PacketPlayOutBlockChange
     */
    public void runUpdate(WrapperPlayServerBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();

            // Updating block information
        player.runKeepaliveAction(k -> {
            IntVector pos = new IntVector(packet.getBlockPosition());

            updateBlock(pos.getX(), pos.getY(), pos.getZ(), packet.getBlockState());
        });
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param packet Wrapped PacketPlayOutMultiBlockChange
     */
    public void runUpdate(WrapperPlayServerMultiBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();
        player.runKeepaliveAction(k -> {
            for (WrapperPlayServerMultiBlockChange.EncodedBlock info : packet.getBlocks()) {

                WrappedBlockState blockState = info.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

                updateBlock(info.getX(), info.getY(), info.getZ(), blockState);
            }
        });
    }

    private void updateBlock(int x, int y, int z, WrappedBlockState blockState) {
        KColumn col = getChunk(x, z);

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
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param chunkUpdate Wrapped PacketPlayOutMapChunk
     */
    public void runUpdate(WrapperPlayServerChunkData chunkUpdate) {
        player.runKeepaliveAction(k -> updateChunk(chunkUpdate.getColumn()));
    }

    public void runUpdate(WrapperPlayServerChunkDataBulk chunkBulk) {
        player.runKeepaliveAction(k -> {
            for (int index = 0; index < chunkBulk.getChunks().length; index++) {
                BaseChunk[] chunks = chunkBulk.getChunks()[index];

                int x = chunkBulk.getX()[index];
                int z = chunkBulk.getZ()[index];

                Column column = new Column(x, z, true, chunks, new TileEntity[0], chunkBulk.getBiomeData()[index]);

                updateChunk(column);
            }
        });
    }

    /**
     * Get the block relative to the specified location
     * @param location the location
     * @param modX the x modifier
     * @param modY the y modifier
     * @param modZ the z modifier
     * @return the block relative to the specified location
     */
    public WrappedBlock getRelative(IntVector location, int modX, int modY, int modZ) {
        return getBlock(location.clone().add(modX, modY, modZ));
    }

    /**
     * Get the block relative to the specified location
     * @param location the location
     * @param face the face
     * @param distance the distance
     * @return the block relative to the specified location
     */
    public WrappedBlock getRelative(IntVector location, BlockFace face, int distance) {
        return getRelative(location,
                face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    /**
     * Get the block relative to the specified location
     * @param location the location
     * @param face the face
     * @return the block relative to the specified location
     */
    public WrappedBlock getRelative(IntVector location, BlockFace face) {
        return getBlock(location.clone().add(face.getModX(), face.getModY(), face.getModZ()));
    }

    public record KColumn(int x, int z, BaseChunk[] chunks) {}

}