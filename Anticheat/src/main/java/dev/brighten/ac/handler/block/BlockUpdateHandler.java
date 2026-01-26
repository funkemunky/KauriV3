package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.world.types.RayCollision;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class BlockUpdateHandler {


    private final APlayer player;
    private final Map<String, World> worlds = new HashMap<>();
    @Getter
    private AtomicReference<World> currentWorld;

    /**
     * Clear all chunks when the player changes worlds
     */
    public void onRespawn(WrapperPlayServerRespawn respawn) {
        respawn.getWorldName()
                .filter(name -> !name.equals(currentWorld.get().getName()))
                .ifPresent(worldName -> {
                    currentWorld.set(getWorldByName(worldName));
                    player.getMob().despawn();

                    player.runKeepaliveAction(ka -> {
                        KLocation origin = player.getMovement().getTo().getLoc().add(0, 1.7, 0);

                        RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection().multiply(-1));

                        Vector3d loc1 = coll.collisionPoint(1.2);
                        player.getMob().spawn(true, new KLocation(loc1), new ArrayList<>(), player);
                    }, 1);
                });
    }

    private World getWorldByName(String name) {
        return worlds.values().stream()
                .filter(world -> world.getName().equals(name))
                .findFirst()
                .orElse(worlds.put(name, new World(name)));
    }

    /**
     * Keep track of block placements since the Bukkit API will be a bit behind
     *
     * @param place wrapped PacketPlayInBlockPlace
     */
    public void onPlace(WrapperPlayClientPlayerBlockPlacement place) {
        player.getInfo().lastBlockUpdate.reset();
        // Could not possibly be a block placement as it's not a block a player is holding.
        Vector3i pos = place.getBlockPosition();

        // Some dumbass shit I have to do because Minecraft with Lilypads
        if (place.getItemStack().isPresent()
                && place.getItemStack().get().getType().equals(ItemTypes.LILY_PAD)) {
            RayCollision rayCollision = new RayCollision(player.getMovement().getTo().getLoc()
                    .add(0, player.getEyeHeight(), 0).toVector(),
                    player.getMovement().getTo().getLoc().getDirection());
            WrappedBlock block = rayCollision.getClosestBlockOfType(player, Materials.LIQUID, 5);

            if (block != null) {
                pos = new Vector3i(
                        block.getLocation().getX(),
                        block.getLocation().getY() + 1,
                        block.getLocation().getZ());
            } else return;
        } // Not an actual block place, just an interact
        else if (pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
            return;
        } else {
            pos = pos.with(pos.getX() + place.getFace().getModX(),
                    pos.getY() + place.getFace().getModY(),
                    pos.getZ() + place.getFace().getModZ());
        }

        player.getInfo().getLastPlace().reset();


        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if(place.getItemStack().isEmpty() || place.getItemStack().get().getType().getPlacedType() == null) {
            return;
        }

        currentWorld.get().updateBlock(x, y, z,
                WrappedBlockState.getDefaultState(place.getItemStack().get().getType().getPlacedType()));
    }

    static final WrappedBlockState airBlockState = WrappedBlockState
            .getByGlobalId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), 0);

    /**
     * Keep track of block breaking since the Bukkit API will be a bit behind.
     *
     * @param dig Wrapped PacketPlayInBlockDig
     */
    public void onDig(WrapperPlayClientPlayerDigging dig) {
        player.getInfo().lastBlockUpdate.reset();
        if (dig.getAction() == DiggingAction.FINISHED_DIGGING) {

            Vector3i pos = dig.getBlockPosition();

            if (pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
                return;
            }

            currentWorld.get().updateBlock(pos.getX(), pos.getY(), pos.getZ(), airBlockState);
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
            Vector3i pos = packet.getBlockPosition();

            currentWorld.get().updateBlock(pos.getX(), pos.getY(), pos.getZ(), packet.getBlockState());
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

                WrappedBlockState blockState = info
                        .getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());

                currentWorld.get().updateBlock(info.getX(), info.getY(), info.getZ(), blockState);
            }
        });
    }

    /**
     * Keep track of block updates since the Bukkit API will be a bit behind.
     * @param chunkUpdate Wrapped PacketPlayOutMapChunk
     */
    public void runUpdate(WrapperPlayServerChunkData chunkUpdate) {
        player.runKeepaliveAction(k -> currentWorld.get().updateChunk(chunkUpdate.getColumn()));
    }

    public void runUpdate(WrapperPlayServerChunkDataBulk chunkBulk) {
        player.runKeepaliveAction(k -> {
            for (int index = 0; index < chunkBulk.getChunks().length; index++) {
                BaseChunk[] chunks = chunkBulk.getChunks()[index];

                int x = chunkBulk.getX()[index];
                int z = chunkBulk.getZ()[index];

                Column column = new Column(x, z, true, chunks, new TileEntity[0],
                        chunkBulk.getBiomeData()[index]);

                currentWorld.get().updateChunk(column);
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
    public WrappedBlock getRelative(Vector3i location, int modX, int modY, int modZ) {
        return currentWorld.get().getBlock(location.add(modX, modY, modZ));
    }

    /**
     * Get the block relative to the specified location
     * @param location the location
     * @param face the face
     * @param distance the distance
     * @return the block relative to the specified location
     */
    public WrappedBlock getRelative(Vector3i location, BlockFace face, int distance) {
        return getRelative(location,
                face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    /**
     * Get the block relative to the specified location
     * @param location the location
     * @param face the face
     * @return the block relative to the specified location
     */
    public WrappedBlock getRelative(Vector3i location, BlockFace face) {
        return currentWorld.get().getBlock(location.add(face.getModX(), face.getModY(), face.getModZ()));
    }

    public WrappedBlock getBlock(int x, int y, int z) {
        return currentWorld.get().getBlock(x, y, z);
    }

    public WrappedBlock getBlock(KLocation location) {
        return getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public WrappedBlock getBlock(Vector3i location) {
        return getBlock(location.getX(), location.getY(), location.getZ());
    }

    public record KColumn(int x, int z, BaseChunk[] chunks) {}

}