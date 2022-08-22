package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInFlying implements WPacket {
    private double x, y, z;
    private float yaw, pitch;
    private boolean looked, moved, onGround;

    @Override
    public PacketType getPacketType() {
        return PacketType.FLYING;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public String toString() {
        return "WPacketPlayInFlying{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", looked=" + looked +
                ", moved=" + moved +
                ", onGround=" + onGround +
                '}';
    }
}
