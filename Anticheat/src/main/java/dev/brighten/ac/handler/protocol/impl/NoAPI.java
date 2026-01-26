package dev.brighten.ac.handler.protocol.impl;

import com.github.retrooper.packetevents.PacketEvents;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.protocol.Protocol;

public class NoAPI implements Protocol {

    @Override
    public int getPlayerVersion(APlayer player) {
        return PacketEvents.getAPI().getPlayerManager().getClientVersion(player.getBukkitPlayer()).getProtocolVersion();
    }
}
