package dev.brighten.ac.bukkit;

import dev.brighten.ac.platform.KauriPlayer;
import dev.brighten.ac.platform.KauriPluginExecutor;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class BukkitPlayerExecutor implements KauriPluginExecutor {

    @Override
    public KauriPlayer getPlayer(String name) {
        return new BukkitPlayer(Bukkit.getPlayer(name));
    }

    @Override
    public KauriPlayer getPlayer(UUID uuid) {
        return new BukkitPlayer(Bukkit.getPlayer(uuid));
    }

    @Override
    public Collection<KauriPlayer> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(BukkitPlayer::new)
                .collect(Collectors.toUnmodifiableList());
    }

}
