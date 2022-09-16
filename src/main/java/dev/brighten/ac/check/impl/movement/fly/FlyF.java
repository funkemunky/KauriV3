package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "Fly (F)", checkId = "flyf", type = CheckType.MOVEMENT)
public class FlyF extends Check {
    public FlyF(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved()
                || player.getInfo().blockAbove.isNotPassed(4)
                || player.getInfo().climbTimer.isNotPassed(2)
                || packet.isOnGround()
                || player.getMovement().getMoveTicks() < 3
                || player.getInfo().isGeneralCancel()) return;

        double acceleration = player.getMovement().getDeltaY() - player.getMovement().getLDeltaY();

        if(acceleration < (player.getMovement().getDeltaXZ() < 0.1 ? -0.17 : -0.1)) {
            flag("acceleration=%.4f", acceleration);
        }
    };
}
