package dev.brighten.ac.handler.protocol;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.handler.protocol.impl.NoAPI;
import dev.brighten.ac.handler.protocol.impl.ProtocolSupport;
import dev.brighten.ac.handler.protocol.impl.ViaVersionAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public interface Protocol {
    int getPlayerVersion(Player player);

    static Protocol getProtocol() {
        if(Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            Anticheat.INSTANCE.alog("Using ViaVersion for ProtocolAPI");
            return new ViaVersionAPI();
        } else if(Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport")) {
            Anticheat.INSTANCE.alog("Using ProtocolSupport for ProtocolAPI");
            return new ProtocolSupport();
        } else {
            Anticheat.INSTANCE.alog("Using Vanilla API for ProtocolAPI");
            return  new NoAPI();
        }
    }
}
