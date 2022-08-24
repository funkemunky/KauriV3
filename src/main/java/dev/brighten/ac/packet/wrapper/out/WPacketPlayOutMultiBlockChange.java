package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.math.IntVector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;

@Builder
@Getter
public class WPacketPlayOutMultiBlockChange implements WPacket {
    private int[] chunk;
    private BlockChange[] changes;

    @Override
    public PacketType getPacketType() {
        return PacketType.MULTI_BLOCK_CHANGE;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @AllArgsConstructor
    @Getter
    public static class BlockChange {
        private IntVector location;
        private Material material;

        @Override
        public String toString() {
            return "BlockChange{" +
                    "location=" + location +
                    ", material=" + material +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "WPacketPlayOutMultiBlockChange{" +
                "chunk=" + Arrays.toString(chunk) +
                ", changes=" + Arrays.toString(changes) +
                '}';
    }
}
