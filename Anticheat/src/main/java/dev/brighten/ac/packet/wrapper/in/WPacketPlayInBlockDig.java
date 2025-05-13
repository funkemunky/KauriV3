package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.WrappedEnumDirection;
import dev.brighten.ac.utils.math.IntVector;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayInBlockDig extends WPacket {

    private IntVector blockPos;
    private WrappedEnumDirection direction;
    private EnumPlayerDigType digType;

    @Override
    public PacketType getPacketType() {
        return PacketType.BLOCK_DIG;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public enum EnumPlayerDigType {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM;

        EnumPlayerDigType() {
        }
    }

    @Override
    public String toString() {
        return "WPacketPlayInBlockDig{" +
                "blockPos=" + blockPos +
                ", direction=" + direction +
                ", digType=" + digType +
                '}';
    }
}
