package dev.brighten.ac.packet.wrapper.in;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

@Getter
@Builder
public class WPacketPlayInChat extends WPacket {
    private String message;

    @Override
    public PacketType getPacketType() {
        return PacketType.CHAT;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processChat(this);
    }

    public BaseComponent[] getWrappedMessage() {
        return TextComponent.fromLegacyText(message);
    }
}
