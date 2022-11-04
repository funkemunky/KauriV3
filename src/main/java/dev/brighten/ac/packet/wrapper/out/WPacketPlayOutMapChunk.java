package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.math.IntVector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Map;

@Builder
@Getter
public class WPacketPlayOutMapChunk extends WPacket {

    private Map<IntVector, MinBlock> blocks;

    @Override
    public PacketType getPacketType() {
        return PacketType.MAP_CHUNK;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @AllArgsConstructor
    public static class MinBlock {
        public Material material;
        public byte data;
    }
}
