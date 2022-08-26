package dev.brighten.ac.check.impl.movement.nofall;

import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.annotation.Async;

@CheckData(name = "NoFall (B)", checkId = "nofallb", type = CheckType.MOVEMENT)
public class NoFallB extends Check {

    public NoFallB(APlayer player) {
        super(player);
    }

    private static double divisor = 1. / 64.;

    private int airBuffer, groundBuffer;

    @Async
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(player.getMovement().getLastTeleport().isNotPassed(3)
                || player.getMovement().getMoveTicks() < 2
                || player.getInfo().canFly
                || player.getInfo().creative
                || player.getBlockInfo().miscNear
                || player.getInfo().inVehicle
                || player.getInfo().climbTimer.isNotPassed(3)
                || player.getCreation().isNotPassed(2000L)
                || player.getInfo().slimeTimer.isNotPassed(3)) {
            if(groundBuffer > 0) groundBuffer--;
            if(airBuffer > 0) airBuffer--;
            return; // If we are waiting for them to teleport, don't check.
        }

        // If they are saying they are on the ground
        if(packet.isOnGround()
                && player.getInfo().vehicleSwitch.isPassed(20)
                && !player.getBlockInfo().blocksBelow
                && !player.getBlockInfo().blocksNear
                && !player.getInfo().isServerGround()) {
            groundBuffer+= 2;
            if(groundBuffer > 14) {
                flag("[%s] g=%s;dy=%.4f;ldy=%.4f", groundBuffer, true,
                        player.getMovement().getDeltaY(), player.getMovement().getLDeltaY());
            }
        } else if(groundBuffer > 0) groundBuffer--;

        final boolean dground = Math.abs(player.getMovement().getDeltaY()) % divisor < 1E-4
                && player.getInfo().isNearGround();

        if(!packet.isOnGround() && player.getInfo().vehicleSwitch.isPassed(20)
                && ((player.getInfo().isServerGround() || player.getBlockInfo().blocksBelow)
                && dground && !player.getBlockInfo().onHalfBlock)) {
            if((airBuffer +=10) > 30) {
                flag("[%s] g=%s;dy=%.4f;ldy=%.4f", airBuffer, false,
                        player.getMovement().getDeltaY(), player.getMovement().getLDeltaY());
            }
        } else if(airBuffer > 0) airBuffer-= 4;

        debug("[%s,%s] g=%s;sg-%s;bbelow=%s;dy=%.4f;ldy=%.4f", groundBuffer, airBuffer, packet.isOnGround(),
                player.getInfo().isServerGround(), player.getBlockInfo().blocksBelow, player.getMovement().getDeltaY(),
                player.getMovement().getLDeltaY());
    };
}
