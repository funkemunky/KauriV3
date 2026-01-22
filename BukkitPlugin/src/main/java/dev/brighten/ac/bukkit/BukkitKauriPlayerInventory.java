package dev.brighten.ac.bukkit;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.brighten.ac.api.platform.KauriPlayerInventory;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Arrays;

@RequiredArgsConstructor
public class BukkitKauriPlayerInventory implements KauriPlayerInventory {

    private final Player player;

    @Override
    public ItemStack getItem(int slot) {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getItem(slot));
    }

    @Override
    public ItemStack getItemInHand() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getItemInHand());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getItemInOffHand());
    }

    @Override
    public ItemStack getHelmet() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getHelmet());
    }

    @Override
    public ItemStack getChestplate() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getChestplate());
    }

    @Override
    public ItemStack getLeggings() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getLeggings());
    }

    @Override
    public ItemStack getBoots() {
        return SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getBoots());
    }

    @Override
    public ItemStack[] getItems() {
        return Arrays.stream(player.getInventory().getContents())
                .map(SpigotConversionUtil::fromBukkitItemStack)
                .toArray(ItemStack[]::new);
    }
}
