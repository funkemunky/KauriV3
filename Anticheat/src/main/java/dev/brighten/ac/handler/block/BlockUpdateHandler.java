package dev.brighten.ac.handler.block;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockDig;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutBlockChange;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutMapChunk;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutMultiBlockChange;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.types.RayCollision;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Optional;

@RequiredArgsConstructor
public class BlockUpdateHandler {
    private final Object2ObjectOpenHashMap<IntVector, WrappedBlock> blockInformation = new Object2ObjectOpenHashMap<>();

    private final APlayer player;

    public void onWorldChange() {
        blockInformation.clear();
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

        synchronized (blockInformation) {
            blockInformation.put(pos, new WrappedBlock(pos.toLocation(player.getBukkitPlayer().getWorld()),
                    place.getItemStack().getType(), (byte) 0));
        }
    }

    /**
     * Keep track of block breaking since the Bukkit API will be a bit behind.
     *
     * @param dig Wrapped PacketPlayInBlockDig
     */
    public void onDig(WPacketPlayInBlockDig dig) {
        player.getInfo().lastBlockUpdate.reset();
        if (dig.getDigType() == WPacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
            synchronized (blockInformation) {
                blockInformation.put(dig.getBlockPos(),
                        new WrappedBlock(dig.getBlockPos().toLocation(player.getBukkitPlayer().getWorld()),
                        Material.AIR, (byte) 0));
            }
        }
    }

    public void runUpdate(WPacketPlayOutBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();

        synchronized (blockInformation) {
            // Updating block information
            player.runInstantAction(k -> {
                if (k.isEnd()) {
                    blockInformation.put(packet.getBlockLocation(),
                            new WrappedBlock(packet.getBlockLocation().toLocation(player.getBukkitPlayer().getWorld()),
                                    packet.getMaterial(), packet.getBlockData()));
                }
            });
        }
    }

    public void runUpdate(WPacketPlayOutMultiBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();
        player.runInstantAction(k -> {
            if (k.isEnd()) {
                synchronized (blockInformation) {
                    for (WPacketPlayOutMultiBlockChange.BlockChange info : packet.getChanges()) {
                        WrappedBlock block = new WrappedBlock(info.getLocation()
                                .toLocation(player.getBukkitPlayer().getWorld()),
                                info.getMaterial(), info.getData());
                        blockInformation.put(info.getLocation(),
                                new WrappedBlock(info.getLocation().toLocation(player.getBukkitPlayer().getWorld()),
                                        info.getMaterial(), info.getData()));
                    }
                }
            }
        });
    }

    public void runUpdate(WPacketPlayOutMapChunk chunkUpdate) {
        player.runInstantAction(k -> {
            if(!k.isEnd()) {
                synchronized (blockInformation) {
                    chunkUpdate.getChunk().getBlocks().forEach((vec, mblock) -> {
                        WrappedBlock block = new WrappedBlock(vec.toLocation(player.getBukkitPlayer().getWorld()),
                                mblock.material, mblock.data);
                        blockInformation.put(vec, block);
                    });
                }
            }
        });
    }

    public WrappedBlock getBlock(IntVector loc) {
        synchronized (blockInformation) {
            WrappedBlock block = blockInformation.get(loc);

            if (block == null) {
                Optional<Block> bukkitBlock = BlockUtils.getBlockAsync(
                        new Location(player.getBukkitPlayer().getWorld(), loc.getX(), loc.getY(), loc.getZ()));

                if (bukkitBlock.isPresent()) {
                    Location bloc = bukkitBlock.get().getLocation();
                    IntVector intVec = new IntVector(bloc.getBlockX(), bloc.getBlockY(), bloc.getBlockZ());
                    block = new WrappedBlock(intVec.toLocation(player.getBukkitPlayer().getWorld()),
                            bukkitBlock.get().getType(), bukkitBlock.get().getData());
                    blockInformation.put(loc, block);
                } else
                    block = new WrappedBlock(loc.toLocation(player.getBukkitPlayer().getWorld()), Material.AIR, (byte)0);
            }

            return block;
        }
    }

    public WrappedBlock getRelative(IntVector location, int modX, int modY, int modZ) {
        return getBlock(location.clone().add(modX, modY, modZ));
    }

    public WrappedBlock getRelative(IntVector location, BlockFace face, int distance) {
        return getRelative(location,
                face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    public WrappedBlock getRelative(IntVector location, BlockFace face) {
        return getBlock(location.clone().add(face.getModX(), face.getModY(), face.getModZ()));
    }
}