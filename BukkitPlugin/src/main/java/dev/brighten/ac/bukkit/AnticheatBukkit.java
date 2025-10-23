package dev.brighten.ac.bukkit;

import co.aikar.commands.BukkitCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.bukkit.protocol.ProtocolSupport;
import dev.brighten.ac.handler.protocol.impl.ViaVersionAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class AnticheatBukkit extends JavaPlugin {
    static AnticheatBukkit INSTANCE;

    private BukkitCommandManager commandManager;

    @Getter
    private BukkitAudiences audience;

    public void onLoad() {
        INSTANCE = this;
        getLogger().info("AnticheatBukkit is loading.");
        BukkitPlayerExecutor bukkitPlayerExecutor = new BukkitPlayerExecutor();
        commandManager = new BukkitCommandManager(this);
        Anticheat anticheat = new Anticheat(commandManager, getLogger(),
                bukkitPlayerExecutor, getDataFolder(), new BukkitRunUtils());

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(AnticheatBukkit.INSTANCE,
                new PacketEventsSettings().debug(true).fullStackTrace(true).kickIfTerminated(false)));
    }

    @Override
    public void onEnable() {
        this.audience = BukkitAudiences.create(this);
        if(Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            Anticheat.INSTANCE.alog("Using ViaVersion for ProtocolAPI");
            Anticheat.INSTANCE.setProtocol(new ViaVersionAPI());
        } else if(Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport")) {
            Anticheat.INSTANCE.alog("Using ProtocolSupport for ProtocolAPI");
            Anticheat.INSTANCE.setProtocol(new ProtocolSupport());
        } else {
            Anticheat.INSTANCE.alog("Using Vanilla API for ProtocolAPI");
        }
        this.audience = BukkitAudiences.create(this);
    }

    @Override
    public void onDisable() {
        commandManager.getScheduler().cancelLocaleTask();
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        if (this.audience != null) {
            this.audience.close();
            this.audience = null;
        }
    }
}
