package dev.brighten.ac.packet.wrapper.in;


import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

@Builder
@Getter
public class WPacketPlayInUseEntity extends WPacket {
    private int entityId;
    private Vector vector;
    private EnumEntityUseAction action;

    @Override
    public PacketType getPacketType() {
        return PacketType.USE_ENTITY;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public enum EnumHand {
        MAIN_HAND, OFF_HAND;
    }

    public Entity getEntity(World world) {
        for (Entity entity : world.getEntities()) {
            if(entity.getEntityId() == entityId) {
                return entity;
            }
        }
        return null;
    }

    public enum EnumEntityUseAction {
        INTERACT("INTERACT"),
        ATTACK("ATTACK"),
        INTERACT_AT("INTERACT_AT");

        @Getter
        private String name;

        EnumEntityUseAction(String name) {
            this.name = name;
        }
    }

    @Override
    public String toString() {
        return "WPacketPlayInUseEntity{" +
                "entityId=" + entityId +
                ", vector=" + vector +
                ", action=" + action +
                '}';
    }
}
