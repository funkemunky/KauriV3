package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutEntityEffect extends WPacket {

    private int entityId, effectId, duration;
    private byte amplifier, flags;

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_EFFECT;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public String toString() {
        return "WPacketPlayOutEntityEffect{" +
                "entityId=" + entityId +
                ", effectId=" + effectId +
                ", duration=" + duration +
                ", amplifier=" + amplifier +
                ", flags=" + flags +
                '}';
    }
}
