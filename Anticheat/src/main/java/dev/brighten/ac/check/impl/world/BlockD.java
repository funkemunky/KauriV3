package dev.brighten.ac.check.impl.world;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.BlockData;

import java.util.HashMap;
import java.util.Map;

@CheckData(name = "Block (D)", checkId = "blockc", type = CheckType.INTERACT)
public class BlockD extends Check {
    public BlockD(APlayer player) {
        super(player);
    }

    private final Map<Vector3i, WrappedBlock> blockPlaceLocations = new HashMap<>();
    private final Timer lastMove = new TickTimer(), lastInvalidBlockOverwrite = new TickTimer();
    private int overwrittenCount = 0;

    @Bind
    WAction<WrapperPlayClientPlayerBlockPlacement> placeBlock = packet -> packet.getItemStack()
            .ifPresent(stack -> {
        var placedType = stack.getType().getPlacedType();

        if(placedType == null) {
            debug("Placed type is null for " + stack.getType());
            return;
        }

        var blockpos = packet.getBlockPosition().add(packet.getFace().getModX(),
                packet.getFace().getModY(),
                packet.getFace().getModZ());

        WrappedBlock block = new WrappedBlock(packet.getBlockPosition(),
                placedType,
                WrappedBlockState.getDefaultState(placedType));
        blockPlaceLocations.put(blockpos, block);
    });

    @Bind
    WAction<WrapperPlayServerBlockChange> serverBlockChange = packet -> {
        if(blockPlaceLocations.containsKey(packet.getBlockPosition())) {
            overwrittenCount++;

            player.runKeepaliveAction(ka -> blockPlaceLocations.remove(packet.getBlockPosition()));
        }
    };

    private KLocation originalLocation = null;

    private int flags;

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(!packet.hasPositionChanged()) {
            if (lastMove.isPassed(5) && lastInvalidBlockOverwrite.isPassed(30)) {
                reset();
            }
            return;
        }

        boolean collided = false;

        for (Vector3i vector3i : blockPlaceLocations.keySet()) {
            if(player.getMovement().getTo().getY() < vector3i.getY()) {
                continue; // Skip blocks above the player
            }
            WrappedBlock block = blockPlaceLocations.get(vector3i);
            var box = BlockData.getData(block.getType()).getBox(player, block, player.getPlayerVersion());

            box.draw(ParticleTypes.FLAME, player);

            if(player.getMovement().getTo().getBox().expand(1, 0.001, 1).isCollided(box)
                    || player.getMovement().getFrom().getBox().expand(1, 0.001, 1).isCollided(box)) {
                collided = true;
                break;
            }
        }

        if(collided && overwrittenCount > 0) {
            if(originalLocation == null) {
                originalLocation = player.getMovement().getFrom().getLoc().clone();
            }
            lastInvalidBlockOverwrite.reset();

            if(flags++ > 5) {
                debug("Flagging!");
                player.getBukkitPlayer().teleport(originalLocation.toLocation(player.getBukkitPlayer().getWorld()));
                reset();
            }
            debug("vl=%s | Movement: %.3f | Block Overwritten: %d | Collided: %b",
                    flags, player.getMovement().getDeltaY(), overwrittenCount, collided);
        } else {
            originalLocation = null;
            flags = 0;
        }

        lastMove.reset();
    };

    private void reset() {
        flags = 0;
        blockPlaceLocations.clear();
        overwrittenCount = 0;
        originalLocation = null;
    }

}
