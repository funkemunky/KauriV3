package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WPacketPlayOutEntityVelocity extends WPacket {
    private int entityId;
    private double deltaX, deltaY, deltaZ;

    @Override
    public PacketType getPacketType() {
        return PacketType.VELOCITY;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processVelocity(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutEntityVelocity{" +
                "entityId=" + entityId +
                ", deltaX=" + deltaX +
                ", deltaY=" + deltaY +
                ", deltaZ=" + deltaZ +
                '}';
    }
}
