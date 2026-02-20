package dev.brighten.ac.handler.protocol.impl;

import com.viaversion.viaversion.api.Via;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.protocol.Protocol;

public class ViaVersionAPI implements Protocol {

    @Override
    public int getPlayerVersion(APlayer player) {
        Anticheat.INSTANCE.alog("Getting player version for " + player.getBukkitPlayer().getName());
        var toReturn = Via.getAPI().getPlayerVersion(player.getBukkitPlayer().getUniqueId());
        Anticheat.INSTANCE.alog("Player version for " + player.getBukkitPlayer().getName() + " is " + toReturn);

        return toReturn;
    }
}
