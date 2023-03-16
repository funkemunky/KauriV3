package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutEntityTeleport extends WPacket {

    private int entityId;
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_TELEPORT;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processEntityTeleport(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutEntityTeleport{" +
                "entityId=" + entityId +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", onGround=" + onGround +
                '}';
    }
}
