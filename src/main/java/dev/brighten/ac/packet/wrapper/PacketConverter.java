package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.out.*;

public interface PacketConverter {
    WPacketPlayInFlying processFlying(Object object);
    WPacketPlayInUseEntity processUseEntity(Object object);

    WPacketPlayInAbilities processAbilities(Object object);

    WPacketPlayInArmAnimation processAnimation(Object object);

    WPacketPlayInBlockDig processBlockDig(Object object);

    WPacketPlayInBlockPlace processBlockPlace(Object object);

    WPacketPlayInCloseWindow processCloseWindow(Object object);

    WPacketPlayInEntityAction processEntityAction(Object object);

    WPacketPlayOutEntityEffect processEntityEffect(Object object);

    WPacketPlayOutPosition processServerPosition(Object object);

    WPacketPlayOutAttachEntity processAttach(Object object);

    WPacketPlayOutEntity processOutEntity(Object object);

    WPacketPlayOutEntityTeleport processEntityTeleport(Object object);

    WPacketHandshakingInSetProtocol processHandshakingProtocol(Object object);

    WPacketPlayOutBlockChange processBlockChange(Object object);

    WPacketPlayOutMultiBlockChange processMultiBlockChange(Object object);
}
