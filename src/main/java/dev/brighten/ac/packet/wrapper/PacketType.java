package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.Anticheat;
import lombok.Getter;

import java.util.Optional;

public enum PacketType {

    FLYING("PacketPlayInFlying", "PacketPlayInPosition", "PacketPlayInPositionLook",
            "PacketPlayInLook", "PacketPlayInFlying$PacketPlayInPositionLook",
            "PacketPlayInFlying$PacketPlayInLook", "PacketPlayInFlying$PacketPlayInPosition"),
    USE_ENTITY("PacketPlayInUseEntity"),
    CLIENT_KEEPALIVE("PacketPlayInKeepAlive"),
    WINDOW_CLICK("PacketPlayInWindowClick"),
    STEER_VEHICLE("PacketPlayInSteerVehicle"),
    CLIENT_PAYLOAD("PacketPlayInCustomPayload"),
    CLIENT_HELM_ITEM("PacketPlayInHeldItemSlot"),
    TAB_COMPLETE("PacketPlayInTabComplete"),
    CREATIVE_SLOT("PacketPlayInSetCreativeSlot"),
    UPDATE_SIGN("PacketPlayInUpdateSign"),
    CLIENT_TRANSACTION("PacketPlayInTransaction"),
    CLIENT_ABILITIES("PacketPlayInAbilities"),
    ARM_ANIMATION("PacketPlayInArmAnimation"),
    BLOCK_DIG("PacketPlayInBlockDig"),
    BLOCK_PLACE("PacketPlayInBlockPlace"),
    CHAT("PacketPlayInChat"),
    CLIENT_COMMAND("PacketPlayInCommand"),
    CLIENT_CLOSE_WINDOW("PacketPlayInCloseWindow"),
    ENTITY_ACTION("PacketPlayInEntityAction"),
    ENTITY_EFFECT("PacketPlayOutEntityEffect"),
    SERVER_KEEPALIVE("PacketPlayOutKeepAlive"),
    SERVER_CHAT("PacketPlayOutChat"),
    SERVER_POSITION("PacketPlayOutPosition"),
    SERVER_TRANSACTION("PacketPlayOutTransaction"),
    NAMED_ENTITY_SPAWN("PacketPlayOutNamedEntitySpawn"),
    SPAWN_ENTITY_LIVING("PacketPlayOutSpawnEntityLiving"),
    SPAWN_ENTITY("PacketPlayOutSpawnEntity"),
    SERVER_PAYLOAD("PacketPlayOutCustomPayload"),
    SERVER_ABILITIES("PacketPlayOutAbilities"),
    ENTITY_METADATA("PacketPlayOutEntityMetadata"),
    VELOCITY("PacketPlayOutEntityVelocity"),
    INFO("PacketPlayOutInfo"),
    ENTITY_DESTROY("PacketPlayOutEntityDestroy"),
    SCOREBOARD_DISPLAY_OBJECTIVE("PacketPlayOutScoreboardDisplayObjective"),
    SCOREBOARD_OBJECTIVE("PacketPlayOutScoreboardObjective"),
    ENTITY_HEAD_ROTATION("PacketPlayOutEntityHeadRotation"),
    ENTITY_TELEPORT("PacketPlayOutEntityTeleport"),
    ENTITY("PacketPlayOutEntity"),
    ENTITY_MOVE("PacketPlayOutEntity$PacketPlayOutRelEntityMove", "PacketPlayOutRelEntityMove"),
    ENTITY_MOVELOOK("PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook", "PacketPlayOutRelEntityMoveLook"),
    ENTITY_LOOK("PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntityLook"),
    BLOCK_CHANGE("PacketPlayOutBlockChange"),
    SERVER_CLOSE_WINDOW("PacketPlayOutCloseWindow"),
    SERVER_HELM_ITEM("PacketPlayOutHeldItemSlot"),
    SERVER_TAB_COMPLETE("PacketPlayOutTabComplete"),
    MAP_CHUNK("PacketPlayOutMapChunk"),
    MULTI_BLOCK_CHANGE("PacketPlayOutMultiBlockChange"),
    RESPAWN("PacketPlayOutRespawn"),
    WORLD_PARTICLE("PacketPlayOutWorldParticles"),
    COMMANDS("PacketPlayOutCommands"),
    SERVER_OPEN_WINDOW("PacketPlayOutOpenWindow"),
    SERVER_ENTITY_EFFECT("PacketPlayOutEntityEffect"),
    SET_SLOT("PacketPlayOutSetSlot"),
    EXPLOSION("PacketPlayOutExplosion"),
    ATTACH("PacketPlayOutAttachEntity"),
    LOGIN_HANDSHAKE("PacketHandshakingInSetProtocol"),
    STATUS_PING("PacketStatusInPing"),
    STATUS_START("PacketStatusInStart"),
    LOGIN_START("PacketLoginInStart"),
    REMOVE_EFFECT("PacketPlayOutRemoveEntityEffect"),
    UNKNOWN();

    PacketType(String... packetIds) {
        this.packetId = packetIds;
    }
    @Getter
    private final String[] packetId;

    public static Optional<PacketType> getByPacketId(String packetId) {
        for (PacketType value : values()) {
            for (String s : value.packetId) {
                if(s.equals(packetId))
                    return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static Object processType(PacketType type, Object object) {
        PacketConverter convert = Anticheat.INSTANCE.getPacketProcessor().getPacketConverter();

        switch (type) {
            case FLYING:
                return convert.processFlying(object);
            case BLOCK_DIG:
                return convert.processBlockDig(object);
            case USE_ENTITY:
                return convert.processUseEntity(object);
            case BLOCK_PLACE:
                return convert.processBlockPlace(object);
            case CLIENT_CLOSE_WINDOW:
                return convert.processCloseWindow(object);
            case ARM_ANIMATION:
                return convert.processAnimation(object);
            case ENTITY_ACTION:
                return convert.processEntityAction(object);
            case CLIENT_ABILITIES:
                return convert.processAbilities(object);
            case ENTITY_EFFECT:
                return convert.processEntityEffect(object);
            case SERVER_POSITION:
                return convert.processServerPosition(object);
            case ATTACH:
                return convert.processAttach(object);
            case ENTITY:
            case ENTITY_LOOK:
            case ENTITY_MOVE:
            case ENTITY_MOVELOOK:
                return convert.processOutEntity(object);
            case ENTITY_TELEPORT:
                return convert.processEntityTeleport(object);
            case LOGIN_HANDSHAKE:
                return convert.processHandshakingProtocol(object);
            case BLOCK_CHANGE:
                return convert.processBlockChange(object);
            case MULTI_BLOCK_CHANGE:
                return convert.processMultiBlockChange(object);
            case VELOCITY:
                return convert.processVelocity(object);
            case CHAT:
                return convert.processChat(object);
            case SERVER_ABILITIES:
                return convert.processOutAbilities(object);
            case WORLD_PARTICLE:
                return convert.processParticles(object);
            case NAMED_ENTITY_SPAWN:
                return convert.processNamedEntitySpawn(object);
            case SPAWN_ENTITY_LIVING:
                return convert.processSpawnLiving(object);
            case REMOVE_EFFECT:
                return convert.processRemoveEffect(object);
            default:
                return object;
        }
    }
}

