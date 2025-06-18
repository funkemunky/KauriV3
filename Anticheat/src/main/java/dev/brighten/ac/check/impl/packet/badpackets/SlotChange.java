package dev.brighten.ac.check.impl.packet.badpackets;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "SlotChange", checkId = "slotchange", description = "Detects invalid slot changes.", type = CheckType.BADPACKETS)
public class SlotChange extends Check {
    public SlotChange(APlayer player) {
        super(player);
    }

    private boolean didServerSlot = false;
    private int lastSlot = -1, buffer;

    private static final int BUFFER_MAX = 6;

    @Bind
    WAction<WrapperPlayClientHeldItemChange> heldItemSlot = packet -> {
        if(didServerSlot) {
            didServerSlot = false;
            lastSlot = packet.getSlot();
            return;
        }

        if(packet.getSlot() == lastSlot) {
            if(++buffer > BUFFER_MAX) {
                flag("Bad slot change detected at index %i", packet.getSlot());
            }
            debug("Invalid slot change detected! Slot: %i, buffer: %i/%i", packet.getSlot(), buffer, BUFFER_MAX);
        } else if(buffer > 0) {
            buffer = Math.max(buffer - 3, 0);
        }
    };

    @Bind
    WAction<WrapperPlayServerHeldItemChange> serverSlot = packet -> {
        didServerSlot = true;
    };
}
