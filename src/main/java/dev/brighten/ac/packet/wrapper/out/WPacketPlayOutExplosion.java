package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.math.FloatVector;
import dev.brighten.ac.utils.math.IntVector;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.util.Vector;

@Getter
@Builder
public class WPacketPlayOutExplosion extends WPacket {
    private Vector origin;
    private float radius; // Currently unused in 1.8
    private IntVector[] blocksExploded;
    private FloatVector entityPush;


    @Override
    public PacketType getPacketType() {
        return PacketType.EXPLOSION;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processOutExplosion(this);
    }
}
