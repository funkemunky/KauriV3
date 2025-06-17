package dev.brighten.ac.check.impl.combat.aim;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

@CheckData(name = "Aim (GCD)", checkId = "aimgcd", type = CheckType.COMBAT, maxVersion = ProtocolVersion.V1_21_5)
public class AimGCD extends Check {
    public AimGCD(APlayer player) {
        super(player);
    }

    private float buffer;
    protected Timer lastGrid = new TickTimer(3);

    @Bind
    WAction<WrapperPlayClientPlayerFlying> onFlying = (packet) -> {
        if(!packet.hasRotationChanged()) return;



        if(player.getMovement().getYawGcdList().size() < 40) {
            if(buffer > 0) buffer--;
            return;
        }

        final float deltaYaw = Math.abs(player.getMovement().getDeltaYaw());
        final float deltaPitch = Math.abs(player.getMovement().getDeltaPitch());
        final float deltaX = deltaYaw / player.getMovement().getYawMode(),
                deltaY = deltaPitch / player.getMovement().getPitchMode();

        final double gridX = MathUtils.getGrid(player.getMovement().getYawGcdList()),
                gridY = MathUtils.getGrid(player.getMovement().getPitchGcdList());

        if(gridX < 0.005 || gridY < 0.005) lastGrid.reset();

        if(deltaX > 200 || deltaY > 200) {
            debug("sensitivity instability: mcp=%.4f, cx=%.4f, cy=%.4f, dx=%.1f, dy=%.1f",
                    player.getMovement().getSensitivityMcp(), player.getMovement().getCurrentSensX(),
                    player.getMovement().getCurrentSensY(), deltaX, deltaY);
            if(buffer > 0) buffer--;
            return;
        }

        boolean flagged = false;
        if(player.getMovement().getPitchGCD() < 0.007 && lastGrid.isPassed()
                && !player.getMovement().isCinematic()
                && player.getMovement().getLastHighRate().isNotPassed(3)) {
            if(deltaPitch < 10 && ++buffer > 8) {
                flag("%s", player.getMovement().getPitchGCD());
            }
            flagged = true;
        } else buffer = 0;

        debug((flagged ? Color.Green : "") +"sensitivity: mcp=%.4f, cx=%.4f, cy=%.4f, dx=%.1f, dy=%.1f",
                player.getMovement().getSensitivityMcp(), player.getMovement().getCurrentSensX(),
                player.getMovement().getCurrentSensY(), deltaX, deltaY);
    };
}
