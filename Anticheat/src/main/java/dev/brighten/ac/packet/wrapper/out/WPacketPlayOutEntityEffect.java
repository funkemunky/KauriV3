package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WPacketPlayOutEntityEffect extends WPacket {

    private int entityId, effectId, duration;
    private byte amplifier, flags;
    /* Flags (source: https://wiki.vg/Protocol#Entity_Effect):
        0x01: Is ambient - was the effect spawned from a beacon? All beacon-generated effects are ambient. Ambient effects use a different icon in the HUD (blue border rather than gray). If all effects on an entity are ambient, the "Is potion effect ambient" living metadata field should be set to true. Usually should not be enabled.
    0x02: Show particles - should all particles from this effect be hidden? Effects with particles hidden are not included in the calculation of the effect color, and are not rendered on the HUD (but are still rendered within the inventory). Usually should be enabled.
    0x04: Show icon - should the icon be displayed on the client? Usually should be enabled.
     */

    @Override
    public PacketType getPacketType() {
        return PacketType.ENTITY_EFFECT;
    }

    @Override
    public Object getPacket() {
        return Anticheat.INSTANCE.getPacketProcessor().getPacketConverter().processEntityEffect(this);
    }

    @Override
    public String toString() {
        return "WPacketPlayOutEntityEffect{" +
                "entityId=" + entityId +
                ", effectId=" + effectId +
                ", duration=" + duration +
                ", amplifier=" + amplifier +
                ", flags=" + flags +
                '}';
    }
}
