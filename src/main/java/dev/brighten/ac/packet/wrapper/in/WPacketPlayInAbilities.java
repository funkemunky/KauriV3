package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInAbilities implements WPacket {
    private boolean invulnerable, flying, allowedFlight, creativeMode;
    private float flySpeed, walkSpeed;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_ABILITIES;
    }
}
