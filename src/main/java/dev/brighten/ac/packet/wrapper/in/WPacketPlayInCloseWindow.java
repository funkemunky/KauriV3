package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInCloseWindow implements WPacket {

    private int id;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_CLOSE_WINDOW;
    }
}
