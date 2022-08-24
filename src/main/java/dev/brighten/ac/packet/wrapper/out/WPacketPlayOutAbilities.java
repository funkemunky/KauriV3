package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.PlayerCapabilities;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutAbilities implements WPacket {

    private PlayerCapabilities capabilities;

    @Override
    public PacketType getPacketType() {
        return PacketType.SERVER_ABILITIES;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processOutAbilities(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutAbilities{" +
                "capabilities=" + capabilities +
                '}';
    }
}
