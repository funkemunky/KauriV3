package dev.brighten.ac.check.impl.misc.inventory;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Inventory (Move)", checkId = "inventoryA", type = CheckType.INVENTORY, maxVersion = ProtocolVersion.V1_11)
public class InventoryMove extends Check {

    public InventoryMove(APlayer player) {
        super(player);
    }

    public int buffer;

    @Bind
    public WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(player.getInfo().isGeneralCancel() || player.EMULATOR.getInput() == null) return;

        // Running inventory check
        final float STRAFING = player.EMULATOR.getInput().getStrafing();
        final float FORWARD = player.EMULATOR.getInput().getForward();

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
