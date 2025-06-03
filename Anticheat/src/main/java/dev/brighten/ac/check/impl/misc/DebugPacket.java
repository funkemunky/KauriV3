package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "DebugPacket", description = "To debug packets", checkId = "debugpacket", type = CheckType.EXPLOIT, maxVersion = ProtocolVersion.V1_21_5)
public class DebugPacket extends Check {
    public DebugPacket(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WPacketPlayInFlying> flying = packet ->
            debug("ypos=%.7f", player.getMovement().getTo().getY());
}
