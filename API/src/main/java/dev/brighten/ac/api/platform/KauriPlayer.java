package dev.brighten.ac.api.platform;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.KPotionEffect;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public interface KauriPlayer {
    UUID getUniqueId();

    String getName();

    void sendMessage(String message);

    void sendMessage(Component message);

    KauriInventory getInventory();

    boolean hasPermission(String permission);

    boolean hasPositionEffect(PotionType type);

    List<KPotionEffect> getActivePotionEffects();

    void teleport(String worldName, KLocation location);

    void teleport(KLocation location);

    default int getEntityId() {
        return PacketEvents.getAPI().getPlayerManager().getUser(getUniqueId()).getEntityId();
    }
}
