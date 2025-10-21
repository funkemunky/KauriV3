package dev.brighten.ac.bukkit;

import dev.brighten.ac.platform.KauriInventory;
import dev.brighten.ac.platform.KauriPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlayer implements KauriPlayer {
    private final Player player;
    private final BukkitKauriInventory inventory;

    public BukkitPlayer(Player player) {
        this.player = player;
        this.inventory = new BukkitKauriInventory(player);
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public KauriInventory getInventory() {
        return inventory;
    }
}
