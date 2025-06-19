package dev.brighten.ac.utils.world.types;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.world.CollisionBox;

public interface CollisionFactory {
    CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock block);
}