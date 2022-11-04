package dev.brighten.ac.check.impl.movement.nofall;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;

@CheckData(name = "NoFall (C)", checkId = "nofallc", type = CheckType.MOVEMENT, punishVl = 5, punishable = false, experimental = true)
public class NoFallC extends Check {

    public NoFallC(APlayer player) {
        super(player);
    }

    private double fallDistance, trueFallDistance;

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved())
            return;

        if(player.getInfo().isGeneralCancel() || player.getMovement().getLastTeleport().isNotPassed(1)) {
            fallDistance = trueFallDistance = 0;
            return;
        }

        if(player.getMovement().getDeltaY() > 0) {
            fallDistance = 0;
            trueFallDistance = 0;
        } else {
            if(packet.isOnGround()) {
                fallDistance = 0;
            } else fallDistance+= player.getMovement().getDeltaY();

            if(player.getBlockInfo().blocksBelow && packet.getY() % MathUtils.GROUND_DIVISOR == 0) {
                trueFallDistance = 0;
                fallDistance = 0;
            } else trueFallDistance+= player.getMovement().getDeltaY();

            double delta = MathUtils.getDelta(trueFallDistance, fallDistance);

            if(delta > 0.1 && !player.getInfo().isNearGround()) {
                flag("delta=%.4f;fd=%.4f;tf=%.4f", delta, fallDistance, trueFallDistance);
                fallDistance = trueFallDistance = 0;
                cancel();
            }

            debug("calc=%.3f, true=%.3f", fallDistance, trueFallDistance);
        }
    };
}
