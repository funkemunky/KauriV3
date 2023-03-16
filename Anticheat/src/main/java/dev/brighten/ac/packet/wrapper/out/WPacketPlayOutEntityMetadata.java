package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class WPacketPlayOutEntityMetadata extends WPacket {

    private int entityId;
    private List<WrappedWatchableObject> watchedObjects;

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_METADATA;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processEntityMetadata(this);
    }
}
