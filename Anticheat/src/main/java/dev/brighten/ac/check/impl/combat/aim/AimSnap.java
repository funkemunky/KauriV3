package dev.brighten.ac.check.impl.combat.aim;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Aim (Snap)", checkId = "aimsnap", type = CheckType.COMBAT, maxVersion = ProtocolVersion.V1_21_5)
public class AimSnap extends Check {

    public AimSnap(APlayer player) {
        super(player);
    }

    private float buffer;

    @Bind
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isLooked()) return;

        float deltaYaw = MathUtils.getAngleDelta(player.getMovement().getTo().getYaw(),
                player.getMovement().getFrom().getYaw());

        if(deltaYaw > 320 && player.getMovement().getLDeltaYaw() > 0
                && player.getMovement().getLDeltaYaw() < 30
                && player.getMovement().getTo().getYaw() < 360
                && player.getMovement().getTo().getYaw() > -360
                && player.getMovement().getLastTeleport().isPassed(1)
                && player.getMovement().getSensitivityX() < 0.65) {
            if(++buffer > 1) {
                flag("yaw=%.3f", deltaYaw);
            }
        } else if(buffer > 0) buffer-= 0.005f;

        debug("[%.3f] yaw=%.3f", buffer, deltaYaw);
    };
}
