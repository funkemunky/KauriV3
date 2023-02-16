package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WPacketPlayOutGameStateChange extends WPacket {
    private short reason;
    private float value;

    @Override
    public PacketType getPacketType() {
        return PacketType.GAME_STATE_CHANGE;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processOutGameStateChange(this);
    }
}
