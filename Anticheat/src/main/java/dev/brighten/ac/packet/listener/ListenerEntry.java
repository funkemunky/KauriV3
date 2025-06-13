package dev.brighten.ac.packet.listener;

import dev.brighten.ac.packet.listener.functions.PacketListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventPriority;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
class ListenerEntry {
    private final EventPriority priority;
    private final PacketListener listener;
    private final long id = UUID.randomUUID().getMostSignificantBits();
}
