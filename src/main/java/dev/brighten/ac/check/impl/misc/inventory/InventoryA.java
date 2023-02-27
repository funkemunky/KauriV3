package dev.brighten.ac.check.impl.misc.inventory;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.annotation.Async;

@CheckData(name = "Inventory (Move)", checkId = "inventoryA", type = CheckType.INVENTORY, maxVersion = ProtocolVersion.V1_11)
public class InventoryA extends Check {

    public InventoryA(APlayer player) {
        super(player);
    }

    public int buffer;
    
    @Async
    public WAction<WPacketPlayInFlying> flying = packet -> {
        // Running inventory check
        final int STRAFING = player.EMULATOR.getInput().getStrafing();
        final int FORWARD = player.EMULATOR.getInput().getForward();

        if((STRAFING != 0 || FORWARD != 0)
                && (player.getInfo().isInventoryOpen())) {
            if(buffer++ > 6) {
                buffer = Math.min(8, buffer);
                flag("s=%s f=%s", STRAFING, FORWARD);
            }
        } else if(buffer > 0) buffer--;

        debug("buffer=%d inv=%s s=%.2f f=%.2f", buffer,
                player.getInfo().isInventoryOpen(), STRAFING, FORWARD);
    };
}
