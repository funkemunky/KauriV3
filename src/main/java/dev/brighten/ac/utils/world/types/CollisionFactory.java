package dev.brighten.ac.utils.world.types;

import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.world.CollisionBox;
import org.bukkit.block.Block;

public interface CollisionFactory {
    CollisionBox fetch(ProtocolVersion version, Block block);
}