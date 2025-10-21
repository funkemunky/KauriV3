package dev.brighten.ac.platform;

import java.util.UUID;

public interface KauriPlayer {
    UUID getUniqueId();

    String getName();

    void sendMessage(String message);

    KauriInventory getInventory();
}
