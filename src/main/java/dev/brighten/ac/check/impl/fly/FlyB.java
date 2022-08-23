package dev.brighten.ac.check.impl.fly;

import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

@CheckData(name = "Fly (B)", type = CheckType.MOVEMENT)
public class FlyB extends Check {
    public FlyB(APlayer player) {
        super(player);
    }

    private Timer lastNearGround = new TickTimer();
    private float buffer;

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(player.getInfo().isNearGround()) lastNearGround.reset();
        if(!packet.isMoved() || player.getInfo().isGeneralCancel()) return;

        if(player.getMovement().getDeltaY() - player.getMovement().getLDeltaY() > 0.01
                && player.getMovement().getMoveTicks() > 3
                && player.getInfo().getLastPlace().isPassed(3)
                && lastNearGround.isPassed(2)
                && player.getInfo().lastLiquid.isPassed(1)
                && player.getInfo().climbTimer.isPassed(1)
                && player.getInfo().getVelocity().isPassed(2)
                && player.getMovement().getDeltaY() > 0) {
            if(++buffer > 1) {
                flag("%.4f>-%.4f",
                        player.getMovement().getDeltaY(), player.getMovement().getLDeltaY());
            }
        } else if(buffer > 0) buffer-= 0.05f;
    };
}
