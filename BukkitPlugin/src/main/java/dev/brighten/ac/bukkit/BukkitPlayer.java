package dev.brighten.ac.bukkit;

import dev.brighten.ac.api.KauriPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlayer implements KauriPlayer {
    private final Player player;

    public BukkitPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }
}
