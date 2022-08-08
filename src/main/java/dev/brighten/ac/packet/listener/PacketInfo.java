package dev.brighten.ac.packet.listener;

import dev.brighten.ac.packet.wrapper.PacketType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Getter
public class PacketInfo {
    private final Player player;
    private final Object packet;
    private final PacketType type;
    private final long timestamp;
    @Setter
    private boolean cancelled;
}
