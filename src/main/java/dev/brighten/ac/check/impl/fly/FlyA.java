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
        if(!packet.isMoved() || (player.getMovement().getDeltaXZ() == 0
                && player.getMovement().getDeltaY() == 0)) {
            return;
        }

        boolean onGround = player.getMovement().getTo().isOnGround() && player.getBlockInfo().blocksBelow,
                fromGround = player.getMovement().getFrom().isOnGround();
        double lDeltaY = fromGround ? 0 : player.getMovement().getLDeltaY();
        double predicted = onGround ? lDeltaY : (lDeltaY - 0.08) * mult;

        if(fromGround && !onGround && player.getMovement().getDeltaY() > 0) {
            predicted = MovementUtils.getJumpHeight(player);
        }

        if(Math.abs(predicted) < 0.005 && ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)) {
            predicted = 0;
        }

        if(lastPos.isPassed(60L)) {
            double toCheck = (predicted - 0.08) * mult;

            if(Math.abs(player.getMovement().getDeltaY() - toCheck)
                    < Math.abs(player.getMovement().getDeltaY() - predicted)) {
                predicted = toCheck;
            }
        }

        double deltaPredict = MathUtils.getDelta(player.getMovement().getDeltaY(), predicted);

        if(!player.getInfo().isGeneralCancel()
                && player.getMovement().getTeleportsToConfirm() == 0
                && player.getInfo().getBlockAbove().isPassed(1)
                && !player.getInfo().isOnLadder()
                && player.getInfo().climbTimer.isPassed(2)
                && !player.getBlockInfo().inWeb
                && !player.getBlockInfo().inScaffolding
                && player.getInfo().getLastLiquid().isPassed(2)
                && !player.getBlockInfo().fenceBelow
                && !player.getInfo().isServerGround()
                && !player.getBlockInfo().onHalfBlock
                && player.getInfo().getVelocity().isPassed(1)
                && !player.getBlockInfo().onSlime && deltaPredict > 0.001) {
            if(++buffer > 3) {
                buffer = 3;
                flag("dY=%.3f p=%.3f dx=%.3f", player.getMovement().getDeltaY(), predicted,
                        player.getMovement().getDeltaXZ());
                cancel();
            }
        } else buffer-= buffer > 0 ? 0.25f : 0;

        debug("dY=%.3f p=%.3f dx=%.3f b=%s velocity=%s", player.getMovement().getDeltaY(), predicted,
                player.getMovement().getDeltaXZ(), buffer, player.getInfo().getVelocity().getPassed());

        lastPos.reset();
    }
}
