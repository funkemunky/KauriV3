package dev.brighten.ac.packet.listener;

import dev.brighten.ac.packet.wrapper.PacketType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class PacketInfo {
    @Getter
    private final Player player;
    @Setter
    private Object packet;
    @Getter
    private final PacketType type;
    @Getter
    private final long timestamp;
    @Getter
    @Setter
    private boolean cancelled;

    public PacketInfo(Player player, Object packet, PacketType type, long timestamp) {
        this.player = player;
        this.packet = packet;
        this.type = type;
        this.timestamp = timestamp;
    }

    public <T> T getPacket() {
        return (T) packet;
    }

}
