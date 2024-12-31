package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.val;
import me.hydro.emulator.object.MoveTag;
import me.hydro.emulator.util.Vector;

import java.util.stream.Collectors;

@CheckData(name = "Prediction", checkId = "predictiona", type = CheckType.MOVEMENT, experimental = true,
        punishable = false)
public class Prediction extends Check {
    private float buffer;
    private boolean maybeSkippedPos;
    private int lastFlying, notMoveTicks;
    private final Timer lastSkipPos = new TickTimer();

    public Prediction(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved()) {
            if(++notMoveTicks > 2) {
                return;
            }
        } else notMoveTicks = 0;
        check: {
            if(player.getBlockInfo().onClimbable
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getInfo().isGeneralCancel()) break check;

            double offset = player.EMULATOR.offset();
            int forward = player.EMULATOR.getInput().getForward();
            int strafe = player.EMULATOR.getInput().getStrafing();

            String tags = player.EMULATOR.getTags().stream()
                    .map(td -> String.valueOf(td.getMoveTag()))
                    .collect(Collectors.joining(", "));

            Vector predicted = player.getMovement().predicted();
            
            val from = player.getMovement().getFrom();
            
            double px = predicted.getX() - from.getX(),
                    py = predicted.getY() - from.getY(),
                    pz = predicted.getZ() - from.getZ();

            double totalMotion = px * px + py * py + pz * pz;
            double mx = player.EMULATOR.getMotion().getMotionX(),
                    my = player.EMULATOR.getMotion().getMotionY(),
                    mz = player.EMULATOR.getMotion().getMotionZ();
            if(totalMotion < 9E-4 || (mx * mx + my * my + mz * mz) < 9E-4) {
                lastSkipPos.reset();
            }
            boolean zeroThree = lastSkipPos.isNotPassed(2);
            boolean collided = player.EMULATOR.containsTag(MoveTag.X_COLLIDED)
                    || player.EMULATOR.containsTag(MoveTag.Z_COLLIDED);

            boolean badOffset = offset > (player.getMovement().getDeltaXZ() == 0
                    && lastSkipPos.isNotPassed(4 )
                    ? (zeroThree ? 0.2 : 0.03) : (zeroThree ? 0.05 : (collided ? 0.01 : 0.003)));

            if(badOffset) {
                debug("[%s] dx=%.6f px=%.6f dz=%.6f pz=%.6f dy=%.6f py=%.6f", zeroThree, player.getMovement().getDeltaX(),
                        px, player.getMovement().getDeltaZ(), pz,
                        player.getMovement().getDeltaY(), py);
                KLocation loc = player.getMovement().getFrom().getLoc().clone()
                        .add(px, py, pz);

                if(tags.contains("velocity")) {
                    buffer++;
                }

                if(++buffer > 5) {
                    flag("%s", offset);
                    correctMovement(loc);
                    buffer = 4;
                }
            } else if(buffer > 0) buffer-= 0.05f;
            debug((badOffset ? Color.Red : "") + "offset=%s f=%s s=%s dy=%.4f dpy=%.4f x=%s px=%s [%s] tags=[%s]",
                    offset, forward, strafe, player.getMovement().getDeltaY(),
                    py, player.getMovement().getTo().getX(), predicted.getX(), totalMotion, tags);
        }
    };
}