package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WPacketPlayInClientCommand extends WPacket {

    private WrappedEnumClientCommand command;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_COMMAND;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public enum WrappedEnumClientCommand {
        PERFORM_RESPAWN,
        REQUEST_STATS,
        OPEN_INVENTORY_ACHIEVEMENT;
    }
}
