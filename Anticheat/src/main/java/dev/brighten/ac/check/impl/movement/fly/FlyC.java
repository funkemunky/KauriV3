package dev.brighten.ac.check.impl.movement.fly;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Fly (Acceleration)", checkId = "flyc", type = CheckType.MOVEMENT)
public class FlyC extends Check {
    public FlyC(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(!packet.hasPositionChanged()
                || player.getInfo().blockAbove.isNotPassed(4)
                || player.getInfo().climbTimer.isNotPassed(2)
                || packet.isOnGround()
                || player.getInfo().getVelocity().isNotPassed(4)
                || player.getMovement().getMoveTicks() < 3
                || player.getInfo().isGeneralCancel()) return;

        double acceleration = player.getMovement().getDeltaY() - player.getMovement().getLDeltaY();

        if(acceleration < (player.getMovement().getDeltaXZ() < 0.1 ? -0.17 : -0.1)) {
            flag("acceleration=%.4f", acceleration);
        }
        debug("acceleration=%.4f", acceleration);
    };
}
