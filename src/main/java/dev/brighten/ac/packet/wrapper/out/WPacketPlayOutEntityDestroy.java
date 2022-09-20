package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WPacketPlayOutEntityDestroy extends WPacket {
    private int[] entityIds;

    public WPacketPlayOutEntityDestroy(int[] entityIds) {
        this.entityIds = entityIds;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_DESTROY;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
