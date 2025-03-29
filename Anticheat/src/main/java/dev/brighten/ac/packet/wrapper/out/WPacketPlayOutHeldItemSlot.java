package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutHeldItemSlot extends WPacket {

    private int handIndex;

    @Override
    public PacketType getPacketType() {
        return PacketType.SERVER_HELM_ITEM;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processOutHeldItemSlot(this);
    }
}
