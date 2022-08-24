package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.math.IntVector;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;

@Builder
@Getter
public class WPacketPlayOutBlockChange implements WPacket {

    private IntVector blockLocation;
    private Material material;


    @Override
    public PacketType getPacketType() {
        return PacketType.BLOCK_CHANGE;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public String toString() {
        return "WPacketPlayOutBlockChange{" +
                "blockLocation=" + blockLocation +
                ", material=" + material +
                '}';
    }
}
