package dev.brighten.ac.api.spigot.impl;

import dev.brighten.ac.api.spigot.Player;
import org.bukkit.inventory.ItemStack;

public class ModernPlayer implements Player {

    private final org.bukkit.entity.Player player;

    public ModernPlayer(org.bukkit.entity.Player player) {
        this.player = player;
    }

    @Override
    public ItemStack getItemInHand() {
        return getItemInMainHand();
    }

    @Override
    public ItemStack getItemInMainHand() {
        return player.getInventory().getItemInMainHand();
    }

    @Override
    public ItemStack getItemInOffHand() {
        return player.getInventory().getItemInOffHand();
    }
}
