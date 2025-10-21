package dev.brighten.ac.platform;

import java.util.Collection;
import java.util.UUID;

public interface KauriPluginExecutor {

    KauriPlayer getPlayer(String name);

    KauriPlayer getPlayer(UUID uuid);

    Collection<KauriPlayer> getOnlinePlayers();
}
