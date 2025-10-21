package dev.brighten.ac.bukkit.protocol;

import dev.brighten.ac.api.KauriPlayer;
import dev.brighten.ac.handler.protocol.Protocol;
import protocolsupport.api.ProtocolSupportAPI;

public class ProtocolSupport implements Protocol {

    @Override
    public int getPlayerVersion(KauriPlayer player) {
        return ProtocolSupportAPI.getProtocolVersion(player).getId();
    }
}
