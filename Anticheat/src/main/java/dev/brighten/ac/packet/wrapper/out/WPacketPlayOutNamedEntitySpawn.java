package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.UUID;

@Getter
@Setter
@Builder
public class WPacketPlayOutNamedEntitySpawn extends WPacket {

    private int entityId;
    private UUID uuid;
    private double x, y, z;
    private float yaw, pitch;
    private Material itemInHand;


    @Override
    public PacketType getPacketType() {
        return PacketType.NAMED_ENTITY_SPAWN;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processNamedEntitySpawn(this);
    }
}
