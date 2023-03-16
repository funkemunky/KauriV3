package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutEntity extends WPacket {

    private int id;
    private boolean looked, moved, onGround;
    private double x, y, z;
    private float yaw, pitch;

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processOutEntity(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutEntity{" +
                "id=" + id +
                ", looked=" + looked +
                ", moved=" + moved +
                ", onGround=" + onGround +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
