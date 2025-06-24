package dev.brighten.ac.check.impl.movement.fly;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Fly (Increment)", checkId = "flyd", type = CheckType.MOVEMENT, experimental = true)
public class FlyD extends Check {
    public FlyD(APlayer player) {
        super(player);
    }

    private double totalY;

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flyingPacket = packet -> {
        if(!packet.hasPositionChanged() || player.getMovement().getMoveTicks() <= 2
                || player.getBlockInfo().miscNear || player.getBlockInfo().onSlab
                || player.getBlockInfo().fenceBelow || player.getBlockInfo().fenceNear
                || player.getBlockInfo().onStairs || player.getInfo().isGeneralCancel()) return;
        double deltaY = player.getMovement().getDeltaY();

        if(deltaY > 0)
            totalY+= deltaY;
        else {
            if(totalY % 0.25 < 1E-6 && totalY > 0.6) {
                flag("totalY=%s gt=%s", totalY, player.getMovement().getGroundTicks());
            }
            debug("totalY=%s", totalY);
            totalY = 0;
        }
    };
}
