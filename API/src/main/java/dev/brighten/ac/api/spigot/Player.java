package dev.brighten.ac.api.spigot;

import org.bukkit.inventory.ItemStack;

public interface Player {

    ItemStack getItemInHand();

    ItemStack getItemInMainHand();

    ItemStack getItemInOffHand();
}
