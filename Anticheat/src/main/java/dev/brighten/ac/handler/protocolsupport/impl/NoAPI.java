package dev.brighten.ac.handler.protocolsupport.impl;

import dev.brighten.ac.handler.protocolsupport.Protocol;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import org.bukkit.entity.Player;

public class NoAPI implements Protocol {

    @Override
    public int getPlayerVersion(Player player) {
        return HandlerAbstract.getHandler().getProtocolVersion(player);
    }
}
