package dev.brighten.ac.check.impl.order;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.TimedWAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;

@CheckData(name = "Order (Use)", type = CheckType.ORDER)
public class UseEntity extends Check {

    private long lastFlying;
    private int buffer;

    public UseEntity(APlayer player) {
        super(player);
    }

    TimedWAction<WPacketPlayInUseEntity> useEntity = (packet, timestamp) -> {
        if(timestamp - lastFlying < 10 && player.getLagInfo().getLastPacketDrop().isPassed(1)) {
            if(++buffer > 5) {
                buffer = 6;
                flag("delta=%s", timestamp - lastFlying);
            }
        } else if(buffer > 0) buffer--;
    };

    TimedWAction<WPacketPlayInFlying> flying = (packet, timestamp) -> {
        if(player.getMovement().getLastTeleport().isPassed(0))
            lastFlying = timestamp;
    };
}
