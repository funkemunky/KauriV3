package dev.brighten.ac.packet.wrapper;

import lombok.Getter;

import java.util.Optional;

public enum PacketType {

    FLYING("PacketPlayInFlying", "PacketPlayInPosition", "PacketPlayInPositionLook",
            "PacketPlayInLook", "PacketPlayInFlying$PacketPlayInPositionLook",
            "PacketPlayInFlying$PacketPlayInLook", "PacketPlayInFlying$PacketPlayInPosition"),

    USE_ENTITY("PacketPlayInUseEntity"),

    NONE();

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

    public static class ServerOld {
        private static final String SERVER = "PacketPlayOut";

        public static final String KEEP_ALIVE = SERVER + "KeepAlive";
        public static final String CHAT = SERVER + "Chat";
        public static final String POSITION = SERVER + "Position";
        public static final String TRANSACTION = SERVER + "Transaction";
        public static final String NAMED_ENTITY_SPAWN = SERVER + "NamedEntitySpawn";
        public static final String SPAWN_ENTITY_LIVING = SERVER + "SpawnEntityLiving";
        public static final String SPAWN_ENTITY = SERVER + "SpawnEntity";
        public static final String CUSTOM_PAYLOAD = SERVER + "CustomPayload";
        public static final String ABILITIES = SERVER + "Abilities";
        public static final String ENTITY_METADATA = SERVER + "EntityMetadata";
        public static final String ENTITY_VELOCITY = SERVER + "EntityVelocity";
        public static final String ENTITY_DESTROY = SERVER + "EntityDestroy";
        public static final String SCOREBOARD_DISPLAY_OBJECTIVE = "ScoreboardDisplayObjective";
        public static final String SCOREBOARD_OBJECTIVE = "ScoreboardObjective";
        public static final String ENTITY_HEAD_ROTATION = SERVER + "EntityHeadRotation";
        public static final String ENTITY_TELEPORT = SERVER + "EntityTeleport";
        public static final String ENTITY = SERVER + "Entity";
        public static final String REL_POSITION = ENTITY + "$" + SERVER + "RelEntityMove";
        public static final String REL_POSITION_LOOK = ENTITY + "$" + SERVER + "RelEntityMoveLook";
        public static final String REL_LOOK = ENTITY + "$" + SERVER + "EntityLook";
        public static final String LEGACY_REL_POSITION = SERVER + "RelEntityMove";
        public static final String LEGACY_REL_POSITION_LOOK = SERVER + "RelEntityMoveLook";
        public static final String LEGACY_REL_LOOK = SERVER + "EntityLook";
        public static final String BLOCK_CHANGE = SERVER + "BlockChange";
        public static final String CLOSE_WINDOW = SERVER + "CloseWindow";
        public static final String HELD_ITEM = SERVER + "HeldItemSlot";
        public static final String TAB_COMPLETE = SERVER + "TabComplete";
        public static final String MAP_CHUNK = SERVER + "MapChunk";
        public static final String MULTI_BLOCK_CHANGE = SERVER + "MultiBlockChange";
        public static final String RESPAWN = SERVER + "Respawn";
        public static final String WORLD_PARTICLE = SERVER + "WorldParticles";
        public static final String COMMANDS = SERVER + "Commands";
        public static final String OPEN_WINDOW = SERVER + "OpenWindow";
        public static final String ENTITY_EFFECT = SERVER + "EntityEffect";
        public static final String SET_SLOT =  SERVER + "SetSlot";
        public static final String EXPLOSION = SERVER + "Explosion";
        public static final String ATTACH = SERVER + "AttachEntity";
    }

    public static class Login {
        public static final String HANDSHAKE = "PacketHandshakingInSetProtocol";
        public static final String PING = "PacketStatusInPing";
        public static final String STATUS_START = "PacketStatusInStart";
        public static final String LOGIN_START = "PacketLoginInStart";
    }
}

