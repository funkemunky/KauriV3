package dev.brighten.ac.handler.block;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.types.RayCollision;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class BlockUpdateHandler {
    private final Long2ObjectOpenHashMap<Column> chunks = new Long2ObjectOpenHashMap<>(1000);

    private final APlayer player;

    /**
     * Clear all chunks when the player changes worlds
     */
    public void onWorldChange() {
        synchronized (chunks) {
            chunks.clear();
        }
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
            Block block = rayCollision.getClosestBlockOfType(player.getBukkitPlayer().getWorld(), Materials.LIQUID, 5);

            if (block != null) {
                if (Materials.checkFlag(block.getType(), Materials.WATER)) {
                    pos = new IntVector(block.getX(), block.getY() + 1, block.getZ());
                }
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


        if(place.getItemStack().isPresent()) {
            Column col = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if(pos.getY() < 0 || (y >> 4) > col.getChunks().length) {
                return;
            }


            BaseChunk chunk = col.getChunks()[y >> 4];

            chunk.set(x & 15, y & 15, z & 15,
                    WrappedBlockState.getDefaultState(place.getItemStack().get().getType().getPlacedType()));
        }
    }

    /**
     * Keep track of block diggings since the Bukkit API will be a bit behind
     * @param x x coordinate
     * @param z z coordinate
     * @return the chunk at the specified coordinates
     */
    public Column getChunk(int x, int z) {
        synchronized (chunks) {
            long hash = LongHash.toLong(x, z);
            Column chunk = chunks.get(hash);

            // If the chunk is null, create a new one
            if(chunk == null) {
                return null;
            }

            return chunk;
        }
    }

    private void updateChunk(Column chunk) {
        synchronized (chunks) {
            chunks.put(LongHash.toLong(chunk.getX(), chunk.getZ()), chunk);
        }
    }

    /**
     * Get a block at the specified coordinates
     * @param location the location of the block
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(Location location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private int minHeight = 0;
    private static final ClientVersion blockVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

    private static final WrappedBlockState airBlockState = WrappedBlockState.getByGlobalId(blockVersion, 0);

    /**
     * Get a block at the specified coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(int x, int y, int z) {
        Column col = getChunk(x >> 4, z >> 4);

        y -= minHeight;

        if(col == null) {
            return new WrappedBlock(new IntVector(x, y, z),
                    Material.AIR,
                    airBlockState);
        }
        BaseChunk chunk = col.getChunks()[y >> 4];

        WrappedBlockState state = chunk.get(blockVersion, x & 15, y & 15, z & 15);
        return new WrappedBlock(new IntVector(x, y, z),
                SpigotConversionUtil.toBukkitMaterialData(state).getItemType(),
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

            Column col = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            if(pos.getY() < 0 || (pos.getY() >> 4) > col.getChunks().length) {
                return;
            }


            BaseChunk chunk = col.getChunks()[pos.getY() >> 4];

            chunk.set(pos.getX() & 15, pos.getY() & 15, pos.getY() & 15, airBlockState);
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

            Column col = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            if(pos.getY() < 0 || (pos.getY() >> 4) > col.getChunks().length) {
                return;
            }

            BaseChunk chunk = col.getChunks()[pos.getY() >> 4];

            chunk.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15,
                    packet.getBlockState());
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

                WrappedBlockState blockState = info.getBlockState(player.getClientVersion());
                Column col = getChunk(info.getX() >> 4, info.getZ() >> 4);

                if(info.getY() < 0 || (info.getY() >> 4) > col.getChunks().length) {
                    continue;
                }

                BaseChunk chunk = col.getChunks()[info.getY() >> 4];

                chunk.set(info.getX() & 15, info.getY() & 15, info.getZ() & 15,
                        blockState);
            }
        });
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
}