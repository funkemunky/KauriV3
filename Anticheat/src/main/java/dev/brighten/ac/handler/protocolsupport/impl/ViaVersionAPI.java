package dev.brighten.ac.handler.protocolsupport.impl;

import dev.brighten.ac.handler.protocolsupport.Protocol;
import org.bukkit.entity.Player;
import us.myles.ViaVersion.api.Via;

public class ViaVersionAPI implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        return Via.getAPI().getPlayerVersion(player.getUniqueId());
    }
}
