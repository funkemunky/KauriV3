package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.handler.block.Chunk;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WPacketPlayOutMapChunk extends WPacket {

    private Chunk chunk;

    @Override
    public PacketType getPacketType() {
        return PacketType.MAP_CHUNK;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
