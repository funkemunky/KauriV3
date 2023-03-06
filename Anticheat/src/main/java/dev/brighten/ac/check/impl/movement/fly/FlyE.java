package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "Fly (YPort)", checkId = "flye", type = CheckType.MOVEMENT)
public class FlyE extends Check {

    public FlyE(APlayer player) {
        super(player);
    }

    private int buffer;

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(player.getInfo().getVelocity().isNotPassed(20)
                || player.getMovement().getMoveTicks() == 0
                || player.getInfo().isCreative()
                || player.getBlockInfo().blocksAbove
                || player.getMovement().getTeleportsToConfirm() > 0
                || player.getInfo().isCanFly()
                || player.getInfo().getLastElytra().isNotPassed(5))
            return;

        final double deltaY = player.getMovement().getDeltaY(),
                lDeltaY = player.getMovement().getLDeltaY();

        if(Math.abs(deltaY + lDeltaY) < 0.05 && player.getInfo().lastHalfBlock.isPassed(2)
                && player.getInfo().slimeTimer.isPassed(5)
                && Math.abs(deltaY) > 0.2) {
            buffer+= 15;
            if(buffer > 20) {
                flag("dy=%s ly=%s", deltaY, lDeltaY);
                buffer= 20; // Prevents flagging too much
            }
        } else if(buffer > 0) buffer--;
    };
}
