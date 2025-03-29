package dev.brighten.ac.check.impl.combat.autoclicker;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Autoclicker (E)", checkId = "autoclickere", type = CheckType.AUTOCLICKER, punishable = false, maxVersion = ProtocolVersion.V1_21_4)
public class AuitoclickerE extends Check {
    public AuitoclickerE(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WPacketPlayInFlying> flyingEvent = packet -> {
        boolean looked = packet.isLooked();
        boolean moved = packet.isMoved();
        debug("Flying Packet: %s, %s", looked, moved);
    };

    @Bind
    WAction<WPacketPlayInBlockPlace> placeEvent = packet ->{
        debug("Place packet");
    };

    @Bind
    WAction<WPacketPlayInArmAnimation> animationEvent = packet -> {
        debug("Arm animation packet");
    };

    @Bind
    WAction<WPacketPlayInUseEntity> useEntity = packet -> {
        debug("Use entity packet: %s, %s", packet.getEntityId(), packet.getAction().toString());
    };

    @Bind
    WAction<WPacketPlayInBlockDig> blockDigEvent = packet -> {
        debug("Block dig packet: %s", packet.getDigType().name());
    };
}
