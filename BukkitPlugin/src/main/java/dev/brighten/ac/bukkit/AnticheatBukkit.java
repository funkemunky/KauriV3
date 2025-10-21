package dev.brighten.ac.bukkit;

import co.aikar.commands.BukkitCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.bukkit.protocol.ProtocolSupport;
import dev.brighten.ac.handler.protocol.impl.NoAPI;
import dev.brighten.ac.handler.protocol.impl.ViaVersionAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class AnticheatBukkit extends JavaPlugin {
    static AnticheatBukkit INSTANCE;

    private BukkitCommandManager commandManager;
    private BukkitPlayerExecutor bukkitPlayerExecutor;
    private Anticheat anticheat;
    public void onLoad() {
        INSTANCE = this;
        getLogger().info("AnticheatBukkit is loading.");
        bukkitPlayerExecutor = new BukkitPlayerExecutor();
        commandManager = new BukkitCommandManager(this);
        anticheat = new Anticheat(commandManager, getLogger(), bukkitPlayerExecutor, getDataFolder(), new BukkitRunUtils());
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(AnticheatBukkit.INSTANCE, new PacketEventsSettings().debug(true).fullStackTrace(true).kickIfTerminated(false)));
    }

    @Override
    public void onEnable() {
        if(Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            Anticheat.INSTANCE.alog("Using ViaVersion for ProtocolAPI");
            Anticheat.INSTANCE.setProtocol(new ViaVersionAPI());
        } else if(Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport")) {
            Anticheat.INSTANCE.alog("Using ProtocolSupport for ProtocolAPI");
            Anticheat.INSTANCE.setProtocol(new ProtocolSupport());
        } else {
            Anticheat.INSTANCE.alog("Using Vanilla API for ProtocolAPI");
        }
    }

    @Override
    public void onDisable() {
        commandManager.getScheduler().cancelLocaleTask();
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

    }
}
