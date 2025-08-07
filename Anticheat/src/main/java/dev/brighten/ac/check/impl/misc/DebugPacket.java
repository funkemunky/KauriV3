package dev.brighten.ac.check.impl.misc;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.events.ConfirmedPositionEvent;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "DebugPacket", description = "To debug packets", checkId = "debugpacket", type = CheckType.EXPLOIT)
public class DebugPacket extends Check {
    public DebugPacket(APlayer player) {
        super(player);
    }

    private long lastPos;

    @Bind
    WTimedAction<WrapperPlayServerPlayerPositionAndLook> serverPosition = (event, timestamp) -> {
        lastPos = timestamp;
        debug("[%s] Server Position", timestamp);
    };

    @Bind
    WTimedAction<WrapperPlayClientPlayerFlying> flying = (packet, timestamp) ->
            debug("[%s] Flying", timestamp - lastPos);

    @Bind
    WTimedAction<WrapperPlayClientTeleportConfirm> confirm = (packet, timestamp) ->
            debug("[%s] Teleport Confirm", timestamp - lastPos);

    @Bind
    WAction<ConfirmedPositionEvent> confirmedPosition = (event) ->
        debug("[%s] Confirmed Position", System.currentTimeMillis() - lastPos);
}
