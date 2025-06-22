package dev.brighten.ac.handler.protocol.impl;

import com.github.retrooper.packetevents.PacketEvents;
import dev.brighten.ac.handler.protocol.Protocol;
import org.bukkit.entity.Player;

public class NoAPI implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        return PacketEvents.getAPI().getPlayerManager().getClientVersion(player).getProtocolVersion();
    }
}
