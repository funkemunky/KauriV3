package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.entity.EntityType;

import java.util.List;

@Getter
@Builder
public class WPacketPlayOutSpawnEntityLiving extends WPacket {

    private int entityId;
    private EntityType type;
    private double x, y, z;
    private float yaw, pitch, headYaw;
    private double motionX, motionY, motionZ;
    private List<WrappedWatchableObject> watchedObjects;

    @Override
    public PacketType getPacketType() {
        return PacketType.SPAWN_ENTITY_LIVING;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processSpawnLiving(this);
    }
}
