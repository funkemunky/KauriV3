package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.ComplexCollisionBox;
import dev.brighten.ac.utils.world.types.HexCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

// Literally https://github.com/GrimAnticheat/Grim/blob/2.0/common/src/main/java/ac/grim/grimac/utils/collisions/blocks/PistonHeadCollision.java
public class PistonDickCollision implements CollisionFactory {
    public static final int[] offsetsXForSide = new int[]{0, 0, 0, 0, -1, 1};

    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock block) {
        // 1.13+ clients differentiate short and long, and the short vs long data is stored
        // This works correctly in 1.12-, as in the piston returns as always long
        //
        // Avoid isShort() call pre-1.13
        // Follow the server's version on 1.13+ clients, as that's the correct way to do it
        double longAmount = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) && block.getBlockState().isShort() ? 0 : 4;

        // And 1.9, 1.10 clients always have "long" piston collision boxes - even if the piston is "short"
        // 1.11 and 1.12 clients differentiate short and long piston collision boxes - but I can never get long heads in multiplayer
        // They show up in the debug world, but my client crashes every time I join the debug world in multiplayer in these two version
        // So just group together 1.9-1.12 into all having long pistons
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2) || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2))
            longAmount = 4;


        // 1.8 and 1.7 clients always have "short" piston collision boxes
        // Apply last to overwrite other long amount setters
        if (version.isOlderThan(ClientVersion.V_1_9) || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9))
            longAmount = 0;


        return switch (block.getBlockState().getFacing()) {
            case UP -> new ComplexCollisionBox(
                    new HexCollisionBox(0, 12, 0, 16, 16, 16),
                    new HexCollisionBox(6, 0 - longAmount, 6, 10, 12, 10));
            case NORTH -> new ComplexCollisionBox(
                    new HexCollisionBox(0, 0, 0, 16, 16, 4),
                    new HexCollisionBox(6, 6, 4, 10, 10, 16 + longAmount));
            case SOUTH -> {
                // SOUTH piston is glitched in 1.7 and 1.8, fixed in 1.9
                // Don't bother with short piston boxes as 1.7/1.8 clients don't have them
                if (version.isOlderThanOrEquals(ClientVersion.V_1_8))
                    yield new ComplexCollisionBox(
                            new HexCollisionBox(0, 0, 12, 16, 16, 16),
                            new HexCollisionBox(4, 6, 0, 12, 10, 12));

                yield new ComplexCollisionBox(
                        new HexCollisionBox(0, 0, 12, 16, 16, 16),
                        new HexCollisionBox(6, 6, 0 - longAmount, 10, 10, 12));
            }
            case WEST -> {
                // WEST piston is glitched in 1.7 and 1.8, fixed in 1.9
                // Don't bother with short piston boxes as 1.7/1.8 clients don't have them
                if (version.isOlderThanOrEquals(ClientVersion.V_1_8))
                    yield new ComplexCollisionBox(
                            new HexCollisionBox(0, 0, 0, 4, 16, 16),
                            new HexCollisionBox(6, 4, 4, 10, 12, 16));

                yield new ComplexCollisionBox(
                        new HexCollisionBox(0, 0, 0, 4, 16, 16),
                        new HexCollisionBox(4, 6, 6, 16 + longAmount, 10, 10));
            }
            case EAST -> new ComplexCollisionBox(
                    new HexCollisionBox(12, 0, 0, 16, 16, 16),
                    new HexCollisionBox(0 - longAmount, 6, 4, 12, 10, 12));
            default -> new ComplexCollisionBox(
                    new HexCollisionBox(0, 0, 0, 16, 4, 16),
                    new HexCollisionBox(6, 4, 6, 10, 16 + longAmount, 10));
        };
    }
}
