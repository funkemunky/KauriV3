package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;

@Getter
@Builder
public class WPacketPlayOutWorldParticles implements WPacket {
    private EnumParticle particle;
    private float x, y, z, offsetX, offsetY, offsetZ, speed;
    private int amount;
    private int[] data;
    private boolean longD;

    @Override
    public PacketType getPacketType() {
        return PacketType.WORLD_PARTICLE;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processParticles(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutWorldParticles{" +
                "particle=" + particle +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                ", offsetZ=" + offsetZ +
                ", speed=" + speed +
                ", amount=" + amount +
                ", data=" + Arrays.toString(data) +
                ", longD=" + longD +
                '}';
    }
}
