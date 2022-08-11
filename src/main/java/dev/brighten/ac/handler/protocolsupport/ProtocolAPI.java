package dev.brighten.ac.handler.protocolsupport;

import dev.brighten.ac.handler.protocolsupport.impl.NoAPI;
import dev.brighten.ac.handler.protocolsupport.impl.ProtocolSupport;
import dev.brighten.ac.handler.protocolsupport.impl.ViaVersionAPI;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.Instance;
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
            INSTANCE = new ViaVersionAPI();
        } else if(Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport")) {
            INSTANCE = new ProtocolSupport();
        } else INSTANCE = new NoAPI();
    }
}
