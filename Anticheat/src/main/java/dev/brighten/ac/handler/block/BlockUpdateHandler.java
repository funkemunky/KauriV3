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
import com.github.retrooper.packetevents.util.Vector3i;
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
import dev.brighten.ac.utils.world.types.RayCollision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hydro.emulator.collision.Block;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class BlockUpdateHandler {


    private final APlayer player;
    private List<AtomicReference<World>> worlds = new ArrayList<>();
    private World currentWorld;
    /**
     * Clear all chunks when the player changes worlds
     */
    public void onWorldChange() {
        setMinHeight(player.getDimensionType());
    }

    private AtomicReference<World> getWorldByName(String name) {
        return worlds.stream()
                .filter(world -> world.get().getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
            var world = new AtomicReference<>(new World(null, name));

            worlds.add(world);
            return world;
        });
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
                && BlockUtils.getXMaterial(place.getItemStack().get().getType()).equals(XMaterial.LILY_PAD)) {
            RayCollision rayCollision = new RayCollision(player.getMovement().getTo().getLoc().add(0, player.getEyeHeight(), 0)
                    .toVector(),
                    player.getMovement().getTo().getLoc().getDirection());
            WrappedBlock block = rayCollision.getClosestBlockOfType(player, Materials.LIQUID, 5);

            if (block != null) {
                pos = new Vector3i(block.getLocation().getX(), block.getLocation().getY() + 1, block.getLocation().getZ());
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

        updateBlock(x, y, z, WrappedBlockState.getDefaultState(place.getItemStack().get().getType().getPlacedType()));
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
            Vector3i pos = packet.getBlockPosition();

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
    public WrappedBlock getRelative(Vector3i location, int modX, int modY, int modZ) {
        return getBlock(location.clone().add(modX, modY, modZ));
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
        return getBlock(location.clone().add(face.getModX(), face.getModY(), face.getModZ()));
    }

    public record KColumn(int x, int z, BaseChunk[] chunks) {}

}