package dev.brighten.ac.check.impl.movement.speed;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import me.hydro.emulator.util.Vector;

@CheckData(name = "Prediction", checkId = "predictiona", type = CheckType.MOVEMENT, experimental = true,
        punishable = false, maxVersion = ClientVersion.V_1_21_7)
public class Prediction extends Check {
    private float buffer;
    private int notMoveTicks;
    private final Timer lastSkipPos = new TickTimer();

    public Prediction(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(!packet.hasPositionChanged()) {
            lastSkipPos.reset();
            if(++notMoveTicks > 2) {

                return;
            }
        } else notMoveTicks = 0;
        check: {
            if(player.getBlockInfo().onClimbable
                    || !player.getMovement().getPosLocs().isEmpty()
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getInfo().isGeneralCancel()) break check;

            if(player.EMULATOR.getInput() == null) {
                return;
            }

            double offset = player.EMULATOR.getOffset();
            float forward = player.EMULATOR.getInput().getForward();
            float strafe = player.EMULATOR.getInput().getStrafing();
            String tags = String.join(", ", player.EMULATOR.getTags());
            Vector predicted = player.getMovement().getPredicted();
            
            var from = player.getMovement().getFrom();
            
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

            boolean zeroThree = lastSkipPos.isNotPassed(4) || tags.contains("003");
            //TODO Implement actual proper handling for this
            boolean isSlimeFuck =
                    (player.getInfo().isWasOnSlime() && player.getMovement().getAirTicks() < 4 && player.getMovement().getGroundTicks() < 4)
                            || (tags.contains("slime-block") && zeroThree);
            boolean collided = player.EMULATOR.getTags().contains("x-collided")
                    || player.EMULATOR.getTags().contains("z-collided");

            double threshold = (player.getMovement().getDeltaXZ() == 0
                    ? (zeroThree ? 0.24 : 0.03) : (zeroThree ? 0.03 : (collided ? 0.01 : 0.003)));

            if(isSlimeFuck) {
                threshold = 0.032;
                tags += ",slimeissue";
            }

            boolean badOffset = offset > threshold;

            if(badOffset) {
                debug("[%s] dx=%.6f px=%.6f dz=%.6f pz=%.6f dy=%.6f py=%.6f velocities=%s", zeroThree, player.getMovement().getDeltaX(),
                        px, player.getMovement().getDeltaZ(), pz,
                        player.getMovement().getDeltaY(), py, player.getVelocityHandler().getPossibleVectors().size());
                KLocation loc = player.getMovement().getFrom().getLoc().clone()
                        .add(px, py, pz);

                if(tags.contains("velocity")) {
                    buffer++;
                }

                if(++buffer > 2) {
                    flag("%s [tags=%s]", offset, tags);
                    correctMovement(loc);
                    buffer = Math.min(4, buffer);
                }
            } else if(buffer > 0) buffer-= 0.05f;
            debug((badOffset ? Color.Red : "") + "offset=%s skip_pos=%s f=%s s=%s dy=%.4f dpy=%.4f x=%s px=%s [%s] tags=[%s]",
                    offset, lastSkipPos.getPassed(), forward, strafe, player.getMovement().getDeltaY(),
                    py, player.getMovement().getTo().getX(), predicted.getX(), totalMotion, tags);
        }
    };
}