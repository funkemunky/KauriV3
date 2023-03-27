package dev.brighten.ac.check.impl.combat.aim;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

@CheckData(name = "Aim (B)", checkId = "aimb", type = CheckType.COMBAT, experimental = true)
public class AimB extends Check {
    public AimB(APlayer player) {
        super(player);
    }

    private float lDelta;
    private int bufferA, bufferB;
    private int lastReset;
    private float largestDelta, lastLargeDelta;
    private int totalLookTicks = 0;
    private final Timer lastLargeLook = new TickTimer();

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isLooked()) return;
        final float sensitivity = player.getMovement().getSensitivityMcp();

        if(sensitivity < 0.05) return; // Not stable

        // We want to wait until we get a good sample size of look movements to calculate sensitivity
        if(totalLookTicks < 80) {
            debug("Waiting for sample size... %s", totalLookTicks);
            totalLookTicks++;
            return;
        }

        if(Math.abs(player.getMovement().getDeltaYaw()) > 1 || Math.abs(player.getMovement().getDeltaPitch()) > 1)
            lastLargeLook.reset();

        final float f = sensitivity * 0.6F + 0.2F;
        final float calc = f * f * f * 1.2f;
        final float x = player.getMovement().getTo().getYaw(), y = player.getMovement().getTo().getPitch();
        final float x2 = x - x % calc, y2 = y - y % calc;
        final float deltaX = Math.abs(x - x2), deltaY = Math.abs(y - y2);
        final float totalDelta = MathUtils.round(deltaX + deltaY, 6);

        if(player.getPlayerTick() - lastReset > 40) {
            lastLargeDelta = largestDelta;
            largestDelta = 0;
            lastReset = player.getPlayerTick();
        } else {
            largestDelta = Math.max(largestDelta, Math.abs(player.getMovement().getDeltaYaw()) + Math.abs(player.getMovement().getDeltaPitch()));
        }

        // Type A, checks if player is moving way too similarly; math is too perfect
        if(Math.abs(totalDelta - lDelta) < 0.00004 && lastLargeDelta > 0.65 && lastLargeLook.isNotPassed(5)) {
            if(++bufferA > 60) {
                flag("Type=[A];b=%s", bufferA);
                if(bufferA > 100) bufferA = 100;
            }
        } else bufferA = 0;

        // Type B, checks if player math is too imperfect
        if(Math.abs(totalDelta - lDelta) > 0.005 && !player.getMovement().isCinematic()) {
            if(++bufferB > 25) {
                flag("Type=[B];b=%s", bufferB);
                if(bufferB > 50) bufferB = 50;
            }
        } else if(bufferB > 0) bufferB-= 4;

        debug("a=%s b=%s total: %s sens=%.7f", bufferA, bufferB, totalDelta, sensitivity);

        lDelta = totalDelta;
    };
}
