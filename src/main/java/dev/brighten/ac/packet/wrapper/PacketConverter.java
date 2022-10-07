package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.login.WPacketHandshakingInSetProtocol;
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

    Object processEntityEffect(WPacketPlayOutEntityEffect object);

    WPacketPlayOutPosition processServerPosition(Object object);

    Object processServerPosition(WPacketPlayOutPosition packet);

    WPacketPlayOutAttachEntity processAttach(Object object);

    WPacketPlayOutEntity processOutEntity(Object object);

    Object processOutEntity(WPacketPlayOutEntity packet);

    WPacketPlayOutEntityTeleport processEntityTeleport(Object object);

    Object processEntityTeleport(WPacketPlayOutEntityTeleport packet);

    WPacketHandshakingInSetProtocol processHandshakingProtocol(Object object);

    WPacketPlayOutBlockChange processBlockChange(Object object);

    WPacketPlayOutMultiBlockChange processMultiBlockChange(Object object);

    WPacketPlayOutEntityVelocity processVelocity(Object object);

    WPacketPlayOutAbilities processOutAbilities(Object object);

    Object processOutAbilities(WPacketPlayOutAbilities packet);

    Object processVelocity(WPacketPlayOutEntityVelocity packet);

    WPacketPlayOutWorldParticles processParticles(Object object);

    Object processParticles(WPacketPlayOutWorldParticles packet);

    WPacketPlayInChat processChat(Object object);

    Object processChat(WPacketPlayInChat packet);

    WPacketPlayOutPlayerInfo processInfo(Object object);

    WPacketPlayOutNamedEntitySpawn processNamedEntitySpawn(Object object);

    Object processNamedEntitySpawn(WPacketPlayOutNamedEntitySpawn packet);

    WPacketPlayOutSpawnEntityLiving processSpawnLiving(Object object);

    Object processSpawnLiving(WPacketPlayOutSpawnEntityLiving packet);

    WPacketPlayOutRemoveEntityEffect processRemoveEffect(Object object);

    Object processRemoveEffect(WPacketPlayOutRemoveEntityEffect packet);

    WPacketPlayOutEntityMetadata processEntityMetadata(Object object);

    Object processEntityMetadata(WPacketPlayOutEntityMetadata packet);

    WPacketPlayOutEntityDestroy processEntityDestroy(Object object);

    WPacketPlayInTransaction processClientTransaction(Object object);

    WPacketPlayOutTransaction processServerTransaction(Object object);

    Object processServerTransaction(WPacketPlayOutTransaction packet);
}
