package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInHeldItemSlot extends WPacket {
    private int handIndex;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_HELM_ITEM;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
