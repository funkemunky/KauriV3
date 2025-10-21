package dev.brighten.ac.api.platform;

import com.github.retrooper.packetevents.protocol.potion.PotionEffect;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import dev.brighten.ac.utils.Tuple;

import java.util.List;
import java.util.UUID;

public interface KauriPlayer {
    UUID getUniqueId();

    String getName();

    void sendMessage(String message);

    KauriInventory getInventory();

    boolean hasPermission(String permission);

    boolean hasPositionEffect(PotionType type);

    List<Tuple<PotionType, PotionEffect.Properties>> getActivePotionEffects();
}
