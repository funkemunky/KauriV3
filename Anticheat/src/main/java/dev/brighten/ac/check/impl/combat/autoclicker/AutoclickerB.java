package dev.brighten.ac.check.impl.combat.autoclicker;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInArmAnimation;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "Autoclicker (B)", checkId = "autoclickerb", type = CheckType.AUTOCLICKER)
public class AutoclickerB extends Check {
    public AutoclickerB(APlayer player) {
        super(player);
    }

    private long lastFlying;
    private int buffer;

    WTimedAction<WPacketPlayInFlying> flyingPacket = (packet, timestamp) -> {
        if(player.getMovement().getLastTeleport().isPassed(1))
            lastFlying = timestamp;
    };

    WTimedAction<WPacketPlayInArmAnimation> animation = (packet, timestamp) -> {
        if(timestamp - lastFlying < 10 && player.getLagInfo().getLastPacketDrop().isPassed(1)) {
            if(++buffer > 4) {
                flag("delta=%s", timestamp - lastFlying);
            }
        } else if(buffer > 0) buffer--;

        debug("delta=%sms", timestamp - lastFlying);
    };
}
