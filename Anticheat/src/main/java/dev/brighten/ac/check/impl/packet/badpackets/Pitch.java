package dev.brighten.ac.check.impl.packet.badpackets;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "BadPackets (Pitch)", checkId = "badpacketspitch", type = CheckType.BADPACKETS, punishVl = 1)
public class Pitch extends Check {
    public Pitch(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(packet.isLooked() && Math.abs(packet.getPitch()) > 90) {
            flag("pitch=%.2f", Math.abs(packet.getPitch()));
        }
    };
}
