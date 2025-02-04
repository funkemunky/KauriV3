package dev.brighten.ac.check.impl.packet.badpackets;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "BadPackets (Pitch)", checkId = "badpacketspitch", type = CheckType.BADPACKETS, punishVl = 1, maxVersion = ProtocolVersion.v_1_21_4)
public class Pitch extends Check {
    public Pitch(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(packet.isLooked() && Math.abs(packet.getPitch()) > 90) {
            flag("pitch=%.2f", Math.abs(packet.getPitch()));
        }
    };
}
