package dev.brighten.ac.handler.protocol.impl;

import dev.brighten.ac.handler.protocol.Protocol;
import org.bukkit.entity.Player;
import protocolsupport.api.ProtocolSupportAPI;

public class ProtocolSupport implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        return ProtocolSupportAPI.getProtocolVersion(player).getId();
    }
}
