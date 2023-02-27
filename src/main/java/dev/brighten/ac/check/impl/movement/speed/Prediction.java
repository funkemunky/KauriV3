package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.val;
import me.hydro.emulator.util.Vector;

@CheckData(name = "Prediction", checkId = "predictiona", type = CheckType.MOVEMENT, experimental = true,
        punishable = false)
public class Prediction extends Check {
    private float buffer;
    private boolean maybeSkippedPos;
    private int lastFlying;
    private final Timer lastSkipPos = new TickTimer();

    public Prediction(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flying = packet -> {

        check: {
            if(!packet.isMoved()
                    || player.getMovement().getLastTeleport().isNotPassed(1)
                    || player.getInfo().getVelocity().isNotPassed(2)
                    || player.getBlockInfo().onClimbable
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getInfo().isGeneralCancel()) break check;

            double offset = player.EMULATOR.getOffset();
            int forward = player.EMULATOR.getInput().getForward();
            int strafe = player.EMULATOR.getInput().getStrafing();
            String tags = String.join(", ", player.EMULATOR.getTags());
            Vector predicted = player.getMovement().getPredicted();
            
            val to = player.getMovement().getTo();
            
            double px = MathUtils.getDelta(predicted.getX(), to.getX()), 
                    py = MathUtils.getDelta(predicted.getY(), to.getY()),
                    pz = MathUtils.getDelta(predicted.getZ(), to.getZ());

            double totalMotion = px * px + py * py + pz * pz;
            boolean zeroThree = totalMotion < 9E-4;

            boolean badOffset = offset > (zeroThree ? 0.03 : 5E-9);

            if(badOffset) {
                debug("[%s] dx=%.6f px=%.6f dz=%.6f pz=%.6f dy=%.6f py=%.6f", zeroThree, player.getMovement().getDeltaX(),
                        px, player.getMovement().getDeltaZ(), pz,
                        player.getMovement().getDeltaY(), py);
                KLocation loc = player.getMovement().getFrom().getLoc().clone()
                        .add(px, py, pz);

                if(++buffer > 5) {
                    flag("%s", offset);
                    correctMovement(loc);
                    buffer = 4;
                }
            } else if(buffer > 0) buffer-= 0.1f;
            debug((badOffset ? Color.Red : "") + "offset=%s f=%s s=%s py=%.3f [%s] tags=[%s]",
                    offset, forward, strafe, py, totalMotion, tags);
        }

        if (ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
            maybeSkippedPos = !packet.isMoved();
        } else {
            if (player.getPlayerTick() - lastFlying > 1) {
                maybeSkippedPos = true;
                debug("maybe skipped pos");
            }
            lastFlying = player.getPlayerTick();
        }

        if (maybeSkippedPos) {
            lastSkipPos.reset();
        }
    };

    private static float roundToFloat(double d) {
        return (float) ((double) Math.round(d * 1.0E8D) / 1.0E8D);
    }
}