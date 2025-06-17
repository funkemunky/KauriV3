package dev.brighten.ac.packet;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMovement;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WPacketPlayOutEntity {
    private int id;
    private boolean looked, moved, onGround;
    private double x, y, z;
    private float yaw, pitch;
    private PacketWrapper<?> packet;

    public WPacketPlayOutEntity(WrapperPlayServerEntityRelativeMoveAndRotation packet) {
        this.id = packet.getEntityId();
        this.x = packet.getDeltaX();
        this.y = packet.getDeltaY();
        this.z = packet.getDeltaZ();
        this.yaw = packet.getYaw();
        this.pitch = packet.getPitch();
        this.onGround = packet.isOnGround();

        this.packet = packet;

        this.looked = true;
        this.moved = true;
    }

    public WPacketPlayOutEntity(WrapperPlayServerEntityRelativeMove packet) {
        this.id = packet.getEntityId();
        this.x = packet.getDeltaX();
        this.y = packet.getDeltaY();
        this.z = packet.getDeltaZ();
        this.onGround = packet.isOnGround();

        this.packet = packet;

        this.looked = false;
        this.moved = true;
    }

    public WPacketPlayOutEntity(WrapperPlayServerEntityRotation packet) {
        this.id = packet.getEntityId();
        this.yaw = packet.getYaw();
        this.pitch = packet.getPitch();
        this.onGround = packet.isOnGround();

        this.packet = packet;

        this.looked = true;
        this.moved = false;
    }

    public WPacketPlayOutEntity(WrapperPlayServerEntityMovement packet) {
        this.id = packet.getEntityId();

        this.looked = false;
        this.moved = false;
    }

    public PacketWrapper<?> getPacket() {
        if(packet != null) {
            return packet;
        }

        if(looked && moved) {
            return new WrapperPlayServerEntityRelativeMoveAndRotation(id, x, y, z, yaw, pitch, onGround);
        } else if(moved) {
            return new WrapperPlayServerEntityRelativeMove(id, x, y, z, onGround);
        } else if(looked) {
            return new WrapperPlayServerEntityRotation(id, yaw, pitch, onGround);
        } else {
            return new WrapperPlayServerEntityMovement(id);
        }
    }

}
