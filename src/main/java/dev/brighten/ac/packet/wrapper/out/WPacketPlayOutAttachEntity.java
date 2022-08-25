package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutAttachEntity extends WPacket {

    private int attachedEntityId, holdingEntityId;
    private boolean isLeashModifer = true;

    @Override
    public PacketType getPacketType() {
        return PacketType.ATTACH;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public String toString() {
        return "WPacketPlayOutAttachEntity{" +
                "attachedEntityId=" + attachedEntityId +
                ", holdingEntityId=" + holdingEntityId +
                ", isLeashModifer=" + isLeashModifer +
                '}';
    }
}
