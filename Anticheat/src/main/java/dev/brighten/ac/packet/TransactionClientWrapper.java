package dev.brighten.ac.packet;


import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import lombok.Getter;

@Getter
public class TransactionClientWrapper {
    private final int action;
    private final int id;

    public TransactionClientWrapper(short action, int id) {
        this.action = action;
        this.id = id;
    }

    public TransactionClientWrapper(PacketReceiveEvent event) {
        if(event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong packet = new WrapperPlayClientPong(event);
            this.action = packet.getId();
            this.id = 0;
        } else if(event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation packet = new WrapperPlayClientWindowConfirmation(event);
            this.action = packet.getActionId();
            this.id = packet.getWindowId();
        } else {
            throw new IllegalArgumentException("Unsupported packet type: " + event.getPacketType());
        }
    }


}
