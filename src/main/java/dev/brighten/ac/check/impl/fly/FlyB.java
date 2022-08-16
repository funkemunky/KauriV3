package dev.brighten.ac.check.impl.fly;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

//@CheckData(name = "Fly (B)", type = CheckType.MOVEMENT)
public class FlyB extends Check {
    public FlyB(APlayer player) {
        super(player);
    }

    private Timer lastNearGround = new TickTimer();

    @Action
    public void onFlying(WPacketPlayInFlying packet) {
        if(getPlayer().getInfo().isNearGround()) lastNearGround.reset();
        if(!packet.isMoved() || getPlayer().getInfo().isGeneralCancel()) return;

        if(getPlayer().getMovement().getDeltaY() - getPlayer().getMovement().getLDeltaY() > 0.01
                && getPlayer().getMovement().getMoveTicks() > 3
                && getPlayer().getInfo().getLastPlace().isPassed(3)
                && lastNearGround.isPassed(2)
                && getPlayer().getInfo().getVelocity().isPassed(2)
                && getPlayer().getMovement().getDeltaY() > 0) {
            flag("%.4f>-%.4f",
                    getPlayer().getMovement().getDeltaY(), getPlayer().getMovement().getLDeltaY());
        }
    }
}
