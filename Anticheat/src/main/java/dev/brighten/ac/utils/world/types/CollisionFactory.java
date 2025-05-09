package dev.brighten.ac.utils.world.types;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.world.CollisionBox;

public interface CollisionFactory {
    CollisionBox fetch(ProtocolVersion version, APlayer player, WrappedBlock block);
}