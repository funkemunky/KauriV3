package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.potion.PotionEffectType;

@Builder
@Getter
public class WPacketPlayOutRemoveEntityEffect extends WPacket {

    private int entityId;
    private PotionEffectType effect;

    @Override
    public PacketType getPacketType() {
        return PacketType.REMOVE_EFFECT;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processRemoveEffect(this);
    }
}
