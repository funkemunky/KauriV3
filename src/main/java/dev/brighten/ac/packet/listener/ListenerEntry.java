package dev.brighten.ac.packet.listener;

import dev.brighten.ac.packet.listener.functions.PacketListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
class ListenerEntry {
    private final Plugin plugin;
    private final EventPriority priority;
    private final PacketListener listener;
    private long id = UUID.randomUUID().getMostSignificantBits();
}
