package dev.brighten.ac.check.impl.combat.autoclicker;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Autoclicker (B)", checkId = "autoclickerb", type = CheckType.AUTOCLICKER)
public class AutoclickerB extends Check {
    public AutoclickerB(APlayer player) {
        super(player);
    }

    private long lastFlying;
    private int buffer;

    @Bind
    WTimedAction<WrapperPlayClientPlayerFlying> flyingPacket = (packet, timestamp) -> {
        if(player.getMovement().getLastTeleport().isPassed(1))
            lastFlying = timestamp;
    };

    @Bind
    WTimedAction<WrapperPlayClientAnimation> animation = (packet, timestamp) -> {
        if(packet.getHand() != InteractionHand.MAIN_HAND) return;

        if(timestamp - lastFlying < 10 && player.getLagInfo().getLastPacketDrop().isPassed(1)) {
            if(++buffer > 4) {
                flag("delta=%s", timestamp - lastFlying);
            }
        } else if(buffer > 0) buffer--;

        debug("delta=%sms", timestamp - lastFlying);
    };
}
