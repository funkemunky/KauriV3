package dev.brighten.ac.check.impl.packet.order;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;

@CheckData(name = "Order (Use)", checkId = "order_use", type = CheckType.ORDER)
public class UseEntity extends Check {

    private long lastFlying;
    private int buffer;

    public UseEntity(APlayer player) {
        super(player);
    }

    WTimedAction<WPacketPlayInUseEntity> useEntity = (packet, timestamp) -> {
        if(timestamp - lastFlying < 10 && player.getLagInfo().getLastPacketDrop().isPassed(1)) {
            if(++buffer > 5) {
                buffer = 6;
                flag("delta=%s", timestamp - lastFlying);
            }
        } else if(buffer > 0) buffer--;

        debug("delta=%s", timestamp - lastFlying);
    };

    WTimedAction<WPacketPlayInFlying> flying = (packet, timestamp) -> {
        if(player.getMovement().getLastTeleport().isPassed(0))
            lastFlying = timestamp;
    };
}
