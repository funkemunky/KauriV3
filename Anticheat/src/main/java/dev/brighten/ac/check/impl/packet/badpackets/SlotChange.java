package dev.brighten.ac.check.impl.packet.badpackets;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInHeldItemSlot;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutHeldItemSlot;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "SlotChange", checkId = "slotchange", description = "Detects invalid slot changes.", type = CheckType.BADPACKETS)
public class SlotChange extends Check {
    public SlotChange(APlayer player) {
        super(player);
    }

    private boolean didServerSlot = false;
    private int lastSlot = -1, buffer;

    private final int BUFFER_MAX = 6;

    @Bind
    WAction<WPacketPlayInHeldItemSlot> heldItemSlot = packet -> {
        if(didServerSlot) {
            didServerSlot = false;
            lastSlot = packet.getHandIndex();
            return;
        }

        if(packet.getHandIndex() == lastSlot) {
            if(++buffer > 6) {
                flag("Bad slot change detected at index %i", packet.getHandIndex());
            }
            debug("Invalid slot change detected! Slot: %i, buffer: %i/%i", packet.getHandIndex(), buffer, BUFFER_MAX);
        } else if(buffer > 0) {
            buffer = Math.max(buffer - 3, 0);
        }
    };

    @Bind
    WAction<WPacketPlayOutHeldItemSlot> serverSlot = packet -> {
        didServerSlot = true;
    };
}
