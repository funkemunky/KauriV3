package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.impl.misc.inventory.InventoryA;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import me.hydro.emulator.object.iteration.Motion;

import java.util.Optional;

@CheckData(name = "Horizontal", checkId = "horizontala", type = CheckType.MOVEMENT, experimental = true)
public class Horizontal extends Check {
    private float buffer;
    private boolean maybeSkippedPos;
    private int lastFlying;
    private final Timer lastSkipPos = new TickTimer();

    public Horizontal(APlayer player) {
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
            Motion predicted = player.getMovement().getPredicted();

            boolean zeroThree =
                    predicted.getMotionX() * predicted.getMotionX()
                            + predicted.getMotionY() * predicted.getMotionY()
                            + predicted.getMotionZ() * predicted.getMotionZ() < 9E-4;

            boolean badOffset = offset > (lastSkipPos.isNotPassed(4) ? 0.03 : 5E-4);

            if(badOffset) {
                debug("[%s] dx=%.6f px=%.6f dz=%.6f pz=%.6f dy=%.6f py=%.6f", zeroThree, player.getMovement().getDeltaX(),
                        predicted.getMotionX(), player.getMovement().getDeltaZ(), predicted.getMotionZ(),
                        player.getMovement().getDeltaY(), predicted.getMotionY());
                KLocation loc = player.getMovement().getFrom().getLoc().clone()
                        .add(predicted.getMotionX(), predicted.getMotionY(), predicted.getMotionZ());

                if(++buffer > 5) {
                    flag("%s", offset);
                    correctMovement(loc);
                    buffer = 4;
                }
            } else if(buffer > 0) buffer-= 0.1f;
            debug((badOffset ? Color.Red : "") + "offset=%s f=%s s=%s py=%.3f tags=[%s]",
                    offset, forward, strafe, predicted.getMotionY(), tags);
        }

        // Running inventory check
        Optional<InventoryA> inventoryA = find(InventoryA.class);

        inventoryA.ifPresent(check -> {

            final int STRAFING = player.EMULATOR.getInput().getStrafing();
            final int FORWARD = player.EMULATOR.getInput().getForward();

            if((STRAFING != 0 || FORWARD != 0)
                    && player.getInfo().isInventoryOpen()) {
                if(check.buffer++ > 6) {
                    check.buffer = Math.min(8, check.buffer);
                    check.flag("s=%s f=%s", STRAFING, FORWARD);
                }
            } else if(check.buffer > 0) check.buffer--;

            check.debug("buffer=%d inv=%s s=%.2f f=%.2f", check.buffer,
                    player.getInfo().isInventoryOpen(), STRAFING, FORWARD);
        });

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