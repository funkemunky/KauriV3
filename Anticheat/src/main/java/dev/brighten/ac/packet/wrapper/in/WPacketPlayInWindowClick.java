package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
@Builder
public class WPacketPlayInWindowClick extends WPacket {

    private int windowId, slot, button, mode;
    private short action;
    private ItemStack clickedItem;

    @Override
    public PacketType getPacketType() {
        return PacketType.WINDOW_CLICK;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
