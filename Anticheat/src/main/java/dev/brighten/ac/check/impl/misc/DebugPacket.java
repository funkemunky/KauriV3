package dev.brighten.ac.check.impl.misc;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "DebugPacket", description = "To debug packets", checkId = "debugpacket", type = CheckType.EXPLOIT, maxVersion = ClientVersion.V_1_21_5)
public class DebugPacket extends Check {
    public DebugPacket(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet ->
            debug("ypos=%.7f", player.getMovement().getTo().getY());
}
