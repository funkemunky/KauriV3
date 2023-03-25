package dev.brighten.ac.check.impl.world;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Block (C)", checkId = "blockc", type = CheckType.INTERACT)
public class BlockC extends Check {

    private long lastPlace;
    private boolean place;
    private float buffer;

    public BlockC(APlayer player) {
        super(player);
    }

    @Bind
    WTimedAction<WPacketPlayInFlying> flying = (packet, timestamp) -> {
        if(player.getInfo().isCreative() || player.getMovement().isExcuseNextFlying()) return;
        if(place) {
            long delta = timestamp - lastPlace;
            if(delta >= 25) {
                if(++buffer >= 10f) {
                    flag("");
                }
            } else if(buffer > 0) buffer-= 0.25f;
            place = false;
        }
    };

    @Bind
    WTimedAction<WPacketPlayInBlockPlace> blockPlace = (packet, timestamp) -> {
        if(player.pastLocations.isEmpty()) return;

        KLocation lastMovePacket = player.pastLocations.getLast().one;

        if(lastMovePacket == null) return;

        final long delta = timestamp - lastMovePacket.timeStamp;

        if(delta <= 25) {
            lastPlace = timestamp;
            place = true;
        } else if(buffer > 0) buffer-= 0.25f;
    };
}
