package dev.brighten.ac.api.platform;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

public interface KauriPlayerInventory {

    ItemStack getItem(int slot);

    ItemStack getItemInHand();

    ItemStack getItemInOffHand();

    ItemStack getHelmet();

    ItemStack getChestplate();

    ItemStack getLeggings();

    ItemStack getBoots();

    ItemStack[] getItems();
}
