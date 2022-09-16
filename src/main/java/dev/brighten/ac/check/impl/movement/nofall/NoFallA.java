package dev.brighten.ac.check.impl.movement.nofall;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "NoFall (A)", checkId = "nofalla", type = CheckType.MOVEMENT)
public class NoFallA extends Check {

    public NoFallA(APlayer player) {
        super(player);
    }

    private static double divisor = 1. / 64.;
    private float buffer;

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved()
                || player.getInfo().isGeneralCancel()
                || (player.getMovement().getDeltaXZ() == 0 && player.getMovement().getDeltaY() == 0)
                || player.getBlockInfo().inLiquid
                || player.getInfo().velocity.isNotPassed(1)
                || player.getMovement().getLastTeleport().isNotPassed(1)) {
            if(buffer > 0) buffer-= 0.5f;
            return;
        }

        boolean onGround = packet.isOnGround();
        boolean flag = false;

        if(onGround) {
            flag = Math.abs(player.getMovement().getDeltaY()) > 0.1
                    && player.getInfo().slimeTimer.isPassed(2)
                    && player.getInfo().getBlockAbove().isPassed(3)
                    && !player.getInfo().isServerGround()
                    && !player.getBlockInfo().fenceNear
                    && (player.getMovement().getDeltaY() >= 0
                    && (Math.abs(player.getMovement().getTo().getLoc().y) % divisor != 0
                    || Math.abs(player.getMovement().getDeltaY()) % divisor != 0)
                    || player.getMovement().getDeltaY() <= player.getMovement().getLDeltaY());
        } else {
            flag = player.getMovement().getDeltaY() == 0 && player.getMovement().getLDeltaY() == 0
                    && player.getInfo().climbTimer.isPassed(3)
                    && player.getInfo().slimeTimer.isPassed(2);
        }

        if(flag) {
            if(++buffer > 1) {
                flag("[%.1f] g=%s;dy=%.4f;ldy=%.4f", buffer, onGround,
                        player.getMovement().getDeltaY(), player.getMovement().getLDeltaY());
            }
        } else if(buffer > 0) buffer-= 0.25f;

        debug("[%.1f] g=%s;dy=%.4f;ldy=%.4f", buffer, onGround,
                player.getMovement().getDeltaY(), player.getMovement().getLDeltaY());
    };
}
