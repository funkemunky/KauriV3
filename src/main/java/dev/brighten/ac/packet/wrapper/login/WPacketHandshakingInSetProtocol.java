package dev.brighten.ac.packet.wrapper.login;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketHandshakingInSetProtocol extends WPacket {
    private int versionNumber, port;
    private String hostname;
    private EnumProtocol protocol;

    @Override
    public PacketType getPacketType() {
        return PacketType.LOGIN_HANDSHAKE;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public enum EnumProtocol {
        HANDSHAKING(-1),
        PLAY(0),
        STATUS(1),
        LOGIN(2),
        UNKNOWN(-69); //Not an actual vanilla object.

        int id;

        EnumProtocol(int id) {
            this.id = id;
        }
    }

    @Override
    public String toString() {
        return "WPacketHandshakingInSetProtocol{" +
                "versionNumber=" + versionNumber +
                ", port=" + port +
                ", hostname='" + hostname + '\'' +
                ", protocol=" + protocol +
                '}';
    }
}
