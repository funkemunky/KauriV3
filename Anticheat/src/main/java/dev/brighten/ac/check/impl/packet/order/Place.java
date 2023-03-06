package dev.brighten.ac.check.impl.packet.order;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "Order (Place)", checkId = "order_place", type = CheckType.ORDER, punishVl = 4)
public class Place extends Check {
    public Place(APlayer player) {
        super(player);
    }

    private long lastFlying;
    private int buffer;

    WTimedAction<WPacketPlayInBlockPlace> placePacket = (packet, timestamp) -> {
        if(timestamp - lastFlying < 10 && player.getLagInfo().getLastPacketDrop().isPassed(1)) {
            if(++buffer > 4) {
                buffer = 5;
                flag("delta=%sms", timestamp - lastFlying);
            }
        } else if(buffer > 0) buffer--;

        debug("buffer=%s delta=%sms", buffer, timestamp - lastFlying);
    };

    WTimedAction<WPacketPlayInFlying> flying = (packet, timestamp) -> {
        if(player.getMovement().getLastTeleport().isPassed(1))
            lastFlying = timestamp;
    };
}
