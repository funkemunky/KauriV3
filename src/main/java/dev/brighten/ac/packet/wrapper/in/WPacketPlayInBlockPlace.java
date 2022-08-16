package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.WrappedEnumDirection;
import dev.brighten.ac.utils.math.IntVector;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
@Builder
public class WPacketPlayInBlockPlace implements WPacket {

    private IntVector blockPos;
    private ItemStack itemStack;
    private WrappedEnumDirection direction;
    private float vecX, vecY, vecZ;

    @Override
    public PacketType getPacketType() {
        return PacketType.BLOCK_PLACE;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
