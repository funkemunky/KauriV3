package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.packet.wrapper.in.*;

public interface PacketConverter {
    WPacketPlayInFlying processFlying(Object object);
    WPacketPlayInUseEntity processUseEntity(Object object);

    WPacketPlayInAbilities processAbilities(Object object);

    WPacketPlayInArmAnimation processAnimation(Object object);

    WPacketPlayInBlockDig processBlockDig(Object object);

    WPacketPlayInBlockPlace processBlockPlace(Object object);

    WPacketPlayInCloseWindow processCloseWindow(Object object);
}
