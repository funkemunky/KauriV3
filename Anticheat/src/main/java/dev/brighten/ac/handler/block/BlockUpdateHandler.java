package dev.brighten.ac.handler.block;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockDig;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutBlockChange;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutMapChunk;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutMultiBlockChange;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.LongHash;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.types.RayCollision;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Optional;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class BlockUpdateHandler {
    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>(1000);

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
    public void onPlace(WPacketPlayInBlockPlace place) {
        player.getInfo().lastBlockUpdate.reset();
        // Could not possibly be a block placement as it's not a block a player is holding.
        IntVector pos = place.getBlockPos().clone();

        // Some dumbass shit I have to do because Minecraft with Lilypads
        if (place.getItemStack() != null && BlockUtils.getXMaterial(place.getItemStack().getType()).equals(XMaterial.LILY_PAD)) {
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
            pos.setX(pos.getX() + place.getDirection().getAdjacentX());
            pos.setY(pos.getY() + place.getDirection().getAdjacentY());
            pos.setZ(pos.getZ() + place.getDirection().getAdjacentZ());
        }

        player.getInfo().getLastPlace().reset();


        Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        chunk.updateBlock(pos, new WrappedBlock(pos.toLocation(player.getBukkitPlayer().getWorld()),
                place.getItemStack().getType(), (byte) 0));
    }

    /**
     * Keep track of block diggings since the Bukkit API will be a bit behind
     * @param x x coordinate
     * @param z z coordinate
     * @return the chunk at the specified coordinates
     */
    public Chunk getChunk(int x, int z) {
        synchronized (chunks) {
            long hash = LongHash.toLong(x, z);
            Chunk chunk = chunks.get(hash);

            // If the chunk is null, create a new one
            if(chunk == null) {
                chunk = new Chunk(x, z);

                chunks.put(hash, chunk);
            }

            return chunk;
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

    /**
     * Get a block at the specified coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(int x, int y, int z) {
        Chunk chunk = getChunk(x >> 4, z >> 4);

        Optional<WrappedBlock> blockOptional = chunk.getBlockAt(x, y, z);

        return blockOptional.orElseGet(() -> new WrappedBlock(new IntVector(x, y, z)
                .toLocation(player.getBukkitPlayer().getWorld()), Material.AIR, (byte) 0));
    }

    /**
     * Get a block at the specified coordinates
     * @param vec the coordinates
     * @return the block at the specified coordinates
     */
    public WrappedBlock getBlock(IntVector vec) {
        return getBlock(vec.getX(), vec.getY(), vec.getZ());
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
    public void onDig(WPacketPlayInBlockDig dig) {
        player.getInfo().lastBlockUpdate.reset();
        if (dig.getDigType() == WPacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {

            IntVector pos = dig.getBlockPos().clone();

            if (pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
                return;
            }

            Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            chunk.updateBlock(pos, new WrappedBlock(pos.toLocation(player.getBukkitPlayer().getWorld()),
                    Material.AIR, (byte) 0));
        }
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param packet Wrapped PacketPlayOutBlockChange
     */
    public void runUpdate(WPacketPlayOutBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();

            // Updating block information
        player.runInstantAction(k -> {
            if (k.isEnd()) {
                IntVector pos = packet.getBlockLocation();

                Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

                chunk.updateBlock(pos, new WrappedBlock(pos.toLocation(player.getBukkitPlayer().getWorld()),
                        packet.getMaterial(), packet.getBlockData()));
            }
        });
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param packet Wrapped PacketPlayOutMultiBlockChange
     */
    public void runUpdate(WPacketPlayOutMultiBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();
        player.runInstantAction(k -> {
            if (k.isEnd()) {
                for (WPacketPlayOutMultiBlockChange.BlockChange info : packet.getChanges()) {
                    WrappedBlock block = new WrappedBlock(info.getLocation()
                            .toLocation(player.getBukkitPlayer().getWorld()),
                            info.getMaterial(), info.getData());

                    IntVector pos = info.getLocation();

                    Chunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4);

                    chunk.updateBlock(pos, block);
                }
            }
        });
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param chunkUpdate Wrapped PacketPlayOutMapChunk
     */
    public void runUpdate(WPacketPlayOutMapChunk chunkUpdate) {
        player.runInstantAction(k -> {
            if(!k.isEnd()) {
                chunkUpdate.getChunk().getBlocks().forEach((vec, mblock) -> {
                    WrappedBlock block = new WrappedBlock(vec.toLocation(player.getBukkitPlayer().getWorld()),
                            mblock.material, mblock.data);

                    Chunk chunk = getChunk(vec.getX() >> 4, vec.getZ() >> 4);

                    chunk.updateBlock(vec, block);
                });
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