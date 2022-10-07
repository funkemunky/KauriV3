package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
public class WPacketPlayInTransaction extends WPacket {
    private int id;
    private short action;
    private boolean accept;

    @Override
    public PacketType getPacketType() {
        return PacketType.CLIENT_TRANSACTION;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WPacketPlayInTransaction that = (WPacketPlayInTransaction) o;
        return id == that.id && action == that.action && accept == that.accept;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, action, accept);
    }

    @Override
    public String toString() {
        return "WPacketPlayInTransaction{" +
                "id=" + id +
                ", action=" + action +
                ", accept=" + accept +
                '}';
    }
}
