package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.annotation.Async;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.List;

@CheckData(name = "Fly (A)", checkId = "flya", type = CheckType.MOVEMENT, experimental = true, punishVl = 7)
public class FlyA extends Check {

    public FlyA(APlayer player) {
        super(player);
    }

    private final Timer lastPos = new MillisTimer();
    private float buffer;
    private static final double mult = 0.98f;
    private boolean didNextPrediction = false;

    @Async
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved() || (player.getMovement().getDeltaXZ() == 0
                && player.getMovement().getDeltaY() == 0)) {
            return;
        }

        // This stuff will false flag the detection and cause a buffer decrease, so we're just going to prevent
        // the check from processing to save resources.
        if(player.getInfo().isGeneralCancel()
                || player.getMovement().getTeleportsToConfirm() > 0
                || player.getInfo().isOnLadder()
                || player.getInfo().climbTimer.isNotPassed(2)
                || player.getBlockInfo().inWeb
                || player.getBlockInfo().inScaffolding
                || player.getInfo().getLastLiquid().isNotPassed(2)
                || player.getBlockInfo().fenceBelow
                || !player.getInfo().worldLoaded
                || player.getBlockInfo().onHalfBlock
                || player.getInfo().getVelocity().isNotPassed(1)
                || player.getBlockInfo().onSlime) {
            if(buffer > 0) buffer-= 0.25f;
            return;
        }

        boolean onGround = player.getMovement().getTo().isOnGround() && player.getBlockInfo().blocksBelow,
                fromGround = player.getMovement().getFrom().isOnGround();
        double lDeltaY = player.getMovement().getLDeltaY();

        // Initial acceleration prediction the vanilla client does
        double predicted = (lDeltaY - 0.08) * mult;

        if(fromGround && !onGround && player.getMovement().getDeltaY() > 0) {
            predicted = MovementUtils.getJumpHeight(player);
        }

        // There will be missed movements that we can't account for if we had to predict the player's next position
        // twice, so we shouldn't flag regardless.
        boolean willBeWeirdNext = didNextPrediction;
        didNextPrediction = false;

        // Since the player skipped a flying packet, the client likely didn't send a small position update
        // This is to go ahead and account for that just in case the >60ms delta is caused by a < 9.0E-4 small movement
        // on all axis. See net.minecraft.client.entity.EntityPlayerSP#onUpdateWalkingPlayer method
        if(lastPos.isPassed(60L)) {
            double toCheck = (predicted - 0.08) * mult;

            if(Math.abs(player.getMovement().getDeltaY() - toCheck)
                    < Math.abs(player.getMovement().getDeltaY() - predicted)) {
                predicted = toCheck;
                didNextPrediction = true;
            }
        }

        // Vanilla wrapping the deltaY to 0 if it's less than a certain amount.
        if(player.getPlayerVersion().isBelow(ProtocolVersion.V1_9)) {
            if(Math.abs(predicted) < 0.005)
                predicted = 0;
        } else if(Math.abs(predicted) < 0.003) {
            predicted = 0;
            debug("Setting y to 0");
        }

        // Vanilla collision algorithm to correct any false positives related to modified deltaY related to ground
        // collision.
        if(player.getBlockInfo().blocksBelow || player.getInfo().isNearGround()) {
            List<SimpleCollisionBox> list = Helper.getCollisions(player,
                    player.getMovement().getFrom().getBox().copy().addCoord(player.getMovement().getDeltaX(), predicted,
                            player.getMovement().getDeltaZ()));
            SimpleCollisionBox axisalignedbb4 = player.getMovement().getFrom().getBox();
            SimpleCollisionBox axisalignedbb5 = axisalignedbb4.copy().addCoord(player.getMovement().getDeltaX(),
                    0.0D, player.getMovement().getDeltaZ());
            double d9 = predicted;

            for (SimpleCollisionBox axisalignedbb6 : list) {
                d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);
            }

            if(predicted != d9) {
                debug("Collided!");
            }

            predicted = d9;
        }

        double deltaPredict = MathUtils.getDelta(player.getMovement().getDeltaY(), predicted);

        // We want it to be at 0.005 since that is the maximum variance from loss of precision
        // We also don't really want to flag if there was a skip in flying related to < 9.0E-4 movement.
        if(!willBeWeirdNext
                && deltaPredict > 0.005) {
            // This slight buffer is to account for anything this check may have not accounted for
            // It is not a permanent fix. However, Java Edition is quite a fickle game to work with; it's quite
            // difficult to reliably check that vanilla behavior is vanilla behavior without weird outliers.
            if(++buffer > 1) {
                buffer = 1;
                flag("dY=%.3f p=%.3f dx=%.3f", player.getMovement().getDeltaY(), predicted,
                        player.getMovement().getDeltaXZ());
                cancel();
            }
        } else buffer-= buffer > 0 ? 0.25f : 0;

        debug("dY=%.3f p=%.3f dx=%.3f b=%s velocity=%s g=%s bbelow=%s ng=%s",
                player.getMovement().getDeltaY(), predicted, player.getMovement().getDeltaXZ(), buffer,
                player.getInfo().getVelocity().getPassed(), packet.isOnGround(), player.getBlockInfo().blocksBelow,
                player.getInfo().isNearGround());

        lastPos.reset();
    };
}
