package dev.brighten.ac.bukkit;

import com.github.retrooper.packetevents.protocol.potion.PotionType;
import dev.brighten.ac.api.platform.KauriInventory;
import dev.brighten.ac.api.platform.KauriPlayer;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.KPotionEffect;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
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
    public void sendMessage(Component message) {
        AnticheatBukkit.INSTANCE.getAudience().player(player).sendMessage(message);
    }

    @Override
    public KauriInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean hasPositionEffect(PotionType type) {
        return player.hasPotionEffect(SpigotConversionUtil.toBukkitPotionEffectType(type));
    }

    @Override
    public List<KPotionEffect> getActivePotionEffects() {
        return player.getActivePotionEffects().stream()
                .map(BukkitUtils::convertBukkitPotionEffect)
                .toList();
    }

    @Override
    public void teleport(String worldName, KLocation location) {
        World world = Bukkit.getWorld(worldName);
        Location loc =  new Location(world, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());

        player.teleport(loc);
    }

    @Override
    public void teleport(KLocation location) {
        Location loc =  new Location(player.getWorld(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        player.teleport(loc);
    }
}
