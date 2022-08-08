package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;

public interface PacketConverter {
    WPacketPlayInFlying processFlying(Object object);
    WPacketPlayInUseEntity processUseEntity(Object object);
}
