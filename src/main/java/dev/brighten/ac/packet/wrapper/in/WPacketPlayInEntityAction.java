package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInEntityAction implements WPacket {

    private EnumPlayerAction action;

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_ACTION;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public static enum EnumPlayerAction {
        START_SNEAKING,
        STOP_SNEAKING,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        RIDING_JUMP,
        OPEN_INVENTORY;

        private EnumPlayerAction() {
        }
    }

    @Override
    public String toString() {
        return "WPacketPlayInEntityAction{" +
                "action=" + action.name() +
                '}';
    }
}
