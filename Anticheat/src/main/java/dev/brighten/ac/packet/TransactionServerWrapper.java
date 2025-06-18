package dev.brighten.ac.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import lombok.RequiredArgsConstructor;

public class TransactionServerWrapper {
    private final short action;
    private final int id;

    public TransactionServerWrapper(short action, int id) {
        this.action = action;
        this.id = id;
    }

    public TransactionServerWrapper(PacketSendEvent event) {
        if(event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing packet = new WrapperPlayServerPing(event);
            this.action = (short) (packet.getId() > Short.MAX_VALUE ? packet.getId() % Short.MAX_VALUE : packet.getId());
            this.id = 0;
        } else if(event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation packet = new WrapperPlayServerWindowConfirmation(event);
            this.action = packet.getActionId();
            this.id = packet.getWindowId();
        } else {
            throw new IllegalArgumentException("Unsupported packet type: " + event.getPacketType());
        }
    }

    public PacketWrapper<?> getWrapper(ClientVersion clientVersion) {
        if(clientVersion.isOlderThan(ClientVersion.V_1_17)) {
            return new WrapperPlayServerWindowConfirmation(id, action, false);
        } else {
            return new WrapperPlayServerPing(action);
        }
    }
}
