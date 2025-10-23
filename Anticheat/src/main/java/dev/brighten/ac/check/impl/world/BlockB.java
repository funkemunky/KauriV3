package dev.brighten.ac.check.impl.world;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;

import java.util.Optional;

@CheckData(name = "Block (B)", checkId = "blockb", type = CheckType.INTERACT)
public class BlockB extends Check {
    public BlockB(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientPlayerBlockPlacement> blockPlaceEvent = event -> {
        Optional<WrappedBlock> ba = BlockUtils.getRelative(player, event.getBlockPosition(), event.getFace());

        WrappedBlock placedBlock = player.getWorldTracker().getBlock(event.getBlockPosition());

        if (ba.isEmpty() || !placedBlock.getType().isSolid()) return;
        double ypos = placedBlock.getLocation().getY() - player.getMovement().getTo().getY();
        double distance = player.getMovement().getTo().getLoc().distance(new KLocation(placedBlock.getLocation().toVector3d()));
        double ab_distance = player.getMovement().getTo().getLoc().distance(new KLocation(ba.get().getLocation().toVector3d())) + 0.4;

        if (distance >= 1.3 && distance > ab_distance && ypos <= 0.5) {
            flag("d:%.4f, ad:%.4f y=%.1f", distance, ab_distance, ypos);
        }
    };
}
