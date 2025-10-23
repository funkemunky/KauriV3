package dev.brighten.ac.handler.block;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public class Chunk {
    @Getter
    private final int x, z;
    private final WrappedBlock[][][] blocks = new WrappedBlock[16][384][16];

    /**
     * Get a block at the specified location
     * @param location - Location of the block
     * @return Optional of the block at the specified location
     */
    public Optional<WrappedBlock> getBlockAt(Vector3i location) {
        return getBlockAt(location.getX(), location.getY(), location.getZ());
    }

    /**
     * @param x - X coordinate of the block
     * @param y - Y coordinate of the block
     * @param z - Z coordinate of the block
     * @return Optional of the block at the specified location
     */
    public Optional<WrappedBlock> getBlockAt(int x, int y, int z) {
       synchronized (blocks) {
           x = x & 15;
           z = z & 15;
           if(y > 320 || y < -64) {
               return Optional.empty();
           }

           if(y < 0) {
               y = Math.abs(y) + 320;
           }
           return Optional.ofNullable(blocks[x][y][z]);
       }
    }

    /**
     * Update a block in the chunk
     * @param x - X coordinate of the block
     * @param y - Y coordinate of the block
     * @param z - Z coordinate of the block
     * @param block - Block to update to
     */
    public void updateBlock(int x, int y, int z, WrappedBlock block) {
        synchronized (blocks) {
            if(y < 0) {
                y = Math.abs(y) + 1000;
            }

            blocks[x & 15][y][z & 15] = block;
        }
    }

    /**
     * Update a block in the chunk
     * @param location - Location of the block
     * @param block - Block to update to
     */
    public void updateBlock(Vector3i location, WrappedBlock block) {
        updateBlock(location.getX(), location.getY(), location.getZ(), block);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Chunk chunk)) return false;
        return x == chunk.x && z == chunk.z && Objects.deepEquals(blocks, chunk.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, Arrays.deepHashCode(blocks));
    }
}
