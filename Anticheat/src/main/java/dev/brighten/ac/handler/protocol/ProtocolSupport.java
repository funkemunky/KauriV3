package dev.brighten.ac.handler.protocol;

import dev.brighten.ac.handler.protocolsupport.Protocol;
import org.bukkit.entity.Player;
import protocolsupport.api.ProtocolSupportAPI;

public class ProtocolSupport implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        return ProtocolSupportAPI.getProtocolVersion(player).getId();
    }
}
