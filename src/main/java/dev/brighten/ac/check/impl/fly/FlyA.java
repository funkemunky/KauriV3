package dev.brighten.ac.check.impl.fly;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;

@CheckData(name = "Fly (A)", type = CheckType.MOVEMENT)
public class FlyA extends Check {

    public FlyA(APlayer player) {
        super(player);
    }

    private Timer lastPos = new MillisTimer();
    private float buffer;
    private static double mult = 0.98f;

    @Action
    public void onFlying(WPacketPlayInFlying packet) {
        if(!packet.isMoved() || (getPlayer().getMovement().getDeltaXZ() == 0
                && getPlayer().getMovement().getDeltaY() == 0)) {
            return;
        }

        boolean onGround = getPlayer().getMovement().getTo().isOnGround() && getPlayer().getBlockInformation().blocksBelow,
                fromGround = getPlayer().getMovement().getFrom().isOnGround();
        double lDeltaY = fromGround ? 0 : getPlayer().getMovement().getLDeltaY();
        double predicted = onGround ? lDeltaY : (lDeltaY - 0.08) * mult;

        if(fromGround && !onGround && getPlayer().getMovement().getDeltaY() > 0) {
            predicted = MovementUtils.getJumpHeight(getPlayer());
        }

        if(Math.abs(predicted) < 0.005 && ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)) {
            predicted = 0;
        }

        if(lastPos.isPassed(60L)) {
            double toCheck = (predicted - 0.08) * mult;

            if(Math.abs(getPlayer().getMovement().getDeltaY() - toCheck)
                    < Math.abs(getPlayer().getMovement().getDeltaY() - predicted)) {
                predicted = toCheck;
            }
        }

        double deltaPredict = MathUtils.getDelta(getPlayer().getMovement().getDeltaY(), predicted);

        if(!getPlayer().getInfo().isGeneralCancel()
                && getPlayer().getInfo().getBlockAbove().isPassed(1)
                && !getPlayer().getInfo().isOnLadder()
                && !getPlayer().getBlockInformation().inWeb
                && !getPlayer().getBlockInformation().inScaffolding
                && !getPlayer().getBlockInformation().inLiquid
                && !getPlayer().getBlockInformation().onHalfBlock
                && getPlayer().getInfo().getVelocity().isPassed(1)
                && !getPlayer().getBlockInformation().onSlime && deltaPredict > 0.016) {
            if(++buffer > 5) {
                buffer = 5;
                flag("dY=%.3f p=%.3f dx=%.3f", getPlayer().getMovement().getDeltaY(), predicted,
                        getPlayer().getMovement().getDeltaXZ());
            }
        } else buffer-= buffer > 0 ? 0.25f : 0;

        debug("dY=%.3f p=%.3f dx=%.3f b=%s velocity=%s", getPlayer().getMovement().getDeltaY(), predicted,
                getPlayer().getMovement().getDeltaXZ(), buffer, getPlayer().getInfo().getVelocity().getPassed());

        lastPos.reset();
    }
}
