package dev.brighten.ac.handler.protocolsupport.impl;

import com.viaversion.viaversion.api.Via;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.handler.protocolsupport.Protocol;
import org.bukkit.entity.Player;

public class ViaVersionAPI implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        Anticheat.INSTANCE.alog("Getting player version for " + player.getName());
        var toReturn = Via.getAPI().getPlayerVersion(player.getUniqueId());
        Anticheat.INSTANCE.alog("Player version for " + player.getName() + " is " + toReturn);

        return toReturn;
    }
}
