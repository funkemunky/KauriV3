package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WPacketPlayOutTransaction extends WPacket {

    private int id;
    private short action;
    private boolean accept;

    @Override
    public PacketType getPacketType() {
        return PacketType.SERVER_TRANSACTION;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processServerTransaction(this);
    }
}
