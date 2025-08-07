package dev.brighten.ac.check.impl.movement.fly;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.List;

@CheckData(name = "Fly (Emulate)", checkId = "flya", type = CheckType.MOVEMENT, punishVl = 7)
public class FlyA extends Check {

    public FlyA(APlayer player) {
        super(player);
    }


    private final Timer LAST_POS = new MillisTimer(), LAST_COLLIDE = new TickTimer();
    private float buffer;
    private boolean didNextPrediction = false;

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(!packet.hasPositionChanged()|| (player.getMovement().getDeltaXZ() == 0
                && player.getMovement().getDeltaY() == 0)) {
            return;
        }

        boolean onGround = player.getMovement().getTo().isOnGround() && player.getBlockInfo().blocksBelow,
                fromGround = player.getMovement().getFrom().isOnGround();
        double lDeltaY = player.getMovement().getLDeltaY();

        // Initial acceleration prediction the vanilla client does
        double DRAG = 0.98f;
        double predicted = (lDeltaY - 0.08) * DRAG;
        
        boolean jumped = false;

        if((fromGround && !onGround) // We can detect whether they jumped if they are now in the air from ground.
                // If they accelerated upward
                || (player.getMovement().getDeltaY() > player.getMovement().getLDeltaY() && !onGround)
                && player.getMovement().getDeltaY() > 0) { //They must be going upward.
            predicted = MovementUtils.getJumpHeight(player);
            jumped = true;
        }

        double threshold = 0.000005;

        // Checking if they collided recently and accounting for the truncated deltaY
        double HIT_BLOCK = 1. / 64.;
        if(LAST_COLLIDE.isNotPassed(2) // Loose to prevent any missed cases
                && player.getMovement().getLDeltaY() > 0
                && player.getMovement().getDeltaY() < player.getMovement().getLDeltaY()
                && player.getMovement().getLDeltaY() % HIT_BLOCK < 0.0126) {
            predicted = -0.08 * DRAG;
            debug("Truncated deltaY");
            threshold = 0.1;
        }

        // There will be missed movements that we can't account for if we had to predict the player's next position
        // twice, so we shouldn't flag regardless.
        boolean willBeWeirdNext = didNextPrediction;
        didNextPrediction = false;


        // Since the player skipped a flying packet, the client likely didn't send a small position update
        // This is to go ahead and account for that just in case the >60ms delta is caused by a < 9.0E-4 small movement
        // on all axis. See net.minecraft.client.entity.EntityPlayerSP#onUpdateWalkingPlayer method
        if(LAST_POS.isPassed(60L)) {
            double toCheck = (predicted - 0.08) * DRAG;

            if(Math.abs(player.getMovement().getDeltaY() - toCheck)
                    < Math.abs(player.getMovement().getDeltaY() - predicted)) {
                predicted = toCheck;
                didNextPrediction = true;
            }
            threshold = 0.2;
        }

        // Vanilla wrapping the deltaY to 0 if it's less than a certain amount.
        if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_21)) {
            if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_9)) {
                if(Math.abs(predicted) < 0.005)
                    predicted = 0;
            } else if(Math.abs(predicted) < 0.003) {
                predicted = 0;
                debug("Setting y to 0");
            }
        }

        // Accounting for the potential vertical velocity
        for (Vector3d possibleVector : player.getVelocityHandler().getPossibleVectors()) {
            double deltaVelocity = MathUtils.getDelta(possibleVector.getY(), player.getMovement().getDeltaY());

            if(deltaVelocity < MathUtils.getDelta(predicted, player.getMovement().getDeltaY())) {
                predicted = possibleVector.getY();
            }

            threshold+= 0.05;
        }

        // Vanilla collision algorithm to correct any false positives related to modified deltaY related to ground
        // collision.
        if(player.getBlockInfo().blocksBelow || player.getBlockInfo().blocksAbove
                || player.getInfo().isNearGround()) {
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
                LAST_COLLIDE.reset(); // Setting the last collide for later use
            }

            predicted = d9;
        }

        // These are all things that can cause potential issues.
        if(player.getInfo().isGeneralCancel()
                || player.getMovement().getTeleportsToConfirm() > 0
                || player.getInfo().isOnLadder()
                || player.getInfo().climbTimer.isNotPassed(2)
                || player.getBlockInfo().inWeb
                || player.getBlockInfo().inScaffolding
                || player.getInfo().getLastLiquid().isNotPassed(2)
                || player.getBlockInfo().fenceBelow
                || !player.getInfo().worldLoaded
                || player.getBlockInfo().onSlime) {
            if(buffer > 0) buffer-= 0.25f;
            debug("Returned");
            return;
        }

        double deltaPredict = MathUtils.getDelta(player.getMovement().getDeltaY(), predicted);

        boolean flagged = false;

        // We want it to be at 0.005 since that is the maximum variance from loss of precision
        // We also don't really want to flag if there was a skip in flying related to < 9.0E-4 movement.
        if(!willBeWeirdNext
                && LAST_COLLIDE.isPassed(1)
                && !player.getInfo().nearGround
                && deltaPredict > threshold) {
            // This slight buffer is to account for anything this check may have not accounted for
            // It is not a permanent fix. However, Java Edition is quite a fickle game to work with; it's quite
            // difficult to reliably check that vanilla behavior is vanilla behavior without weird outliers.
            if(++buffer > 1) {
                buffer = 1;
                flag("dY=%.3f p=%.3f dx=%.3f", player.getMovement().getDeltaY(), predicted,
                        player.getMovement().getDeltaXZ());
                cancel();
            }
            flagged = true;
        } else buffer-= buffer > 0 ? 0.25f : 0;

        debug((flagged ? Color.Green : "")
                        + "[%s] dY=%.3f ldy=%.3f p=%.3f dx=%.3f j=%s velocity=%s fg=%s g=%s lc=%s bbelow=%s ng=%s",
                buffer, player.getMovement().getDeltaY(), player.getMovement().getLDeltaY(),
                predicted, player.getMovement().getDeltaXZ(), jumped,
                player.getInfo().getVelocity().getPassed(), fromGround, onGround, LAST_COLLIDE.getPassed(),
                player.getBlockInfo().blocksBelow, player.getInfo().isNearGround());

        LAST_POS.reset();
    };
}
