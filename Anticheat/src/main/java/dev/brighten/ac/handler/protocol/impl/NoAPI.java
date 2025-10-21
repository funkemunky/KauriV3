package dev.brighten.ac.handler.protocol.impl;

import com.github.retrooper.packetevents.PacketEvents;
import dev.brighten.ac.api.KauriPlayer;
import dev.brighten.ac.api.spigot.Player;
import dev.brighten.ac.handler.protocol.Protocol;

public class NoAPI implements Protocol {

    @Override
    public int getPlayerVersion(KauriPlayer player) {
        return PacketEvents.getAPI().getPlayerManager().getClientVersion(player).getProtocolVersion();
    }
}
