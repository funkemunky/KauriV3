package dev.brighten.ac.handler.protocolsupport;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.handler.protocolsupport.impl.NoAPI;
import dev.brighten.ac.handler.protocolsupport.impl.ProtocolSupport;
import dev.brighten.ac.handler.protocolsupport.impl.ViaVersionAPI;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.annotation.Instance;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

@Init
public class ProtocolAPI {

    public Map<String, Integer> protocolVersionByIP = new HashMap<>();

    public static Protocol INSTANCE;
    @Instance
    public static ProtocolAPI classInstance;

    public ProtocolAPI() {
        if(Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            Anticheat.INSTANCE.alog("Using ViaVersion for ProtocolAPI");
            INSTANCE = new ViaVersionAPI();
        } else if(Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport")) {
            Anticheat.INSTANCE.alog("Using ProtocolSupport for ProtocolAPI");
            INSTANCE = new ProtocolSupport();
        } else {
            Anticheat.INSTANCE.alog("Using Vanilla API for ProtocolAPI");
            INSTANCE = new NoAPI();
        }
    }
}
