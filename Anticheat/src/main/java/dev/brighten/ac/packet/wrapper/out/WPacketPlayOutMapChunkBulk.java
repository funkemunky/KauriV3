package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.handler.block.Chunk;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class WPacketPlayOutMapChunkBulk extends WPacket {

    private List<Chunk> chunks;

    @Override
    public PacketType getPacketType() {
        return PacketType.MAP_CHUNK_BULK;
    }

    @Override
    public Object getPacket() {
        return null;
    }
}
