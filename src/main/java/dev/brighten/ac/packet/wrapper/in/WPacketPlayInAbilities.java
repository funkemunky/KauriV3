package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.PlayerCapabilities;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInAbilities implements WPacket {
    private PlayerCapabilities capabilities;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_ABILITIES;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public String toString() {
        return "WPacketPlayInAbilities{" +
                "capabilities=" + capabilities +
                '}';
    }
}
