package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.impl.misc.inventory.InventoryA;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

import java.util.Optional;

@CheckData(name = "Horizontal", checkId = "horizontala", type = CheckType.MOVEMENT, experimental = true)
public class Horizontal extends Check {
    private float buffer;

    public Horizontal(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flying = packet -> {
        double offset = player.EMULATOR.getOffset();

        debug("offset=%s", offset);

        // Running inventory check
        Optional<InventoryA> inventoryA = find(InventoryA.class);

        inventoryA.ifPresent(check -> {

            final int STRAFING = player.EMULATOR.getInput().getStrafing();
            final int FORWARD = player.EMULATOR.getInput().getForward();

            if((STRAFING != 0 || FORWARD != 0)
                    && player.getInfo().isInventoryOpen()) {
                if(check.buffer++ > 6) {
                    check.buffer = Math.min(8, check.buffer);
                    check.flag("s=%s f=%s", STRAFING, FORWARD);
                }
            } else if(check.buffer > 0) check.buffer--;

            check.debug("buffer=%d inv=%s s=%.2f f=%.2f", check.buffer,
                    player.getInfo().isInventoryOpen(), STRAFING, FORWARD);
        });


    };

    private static float roundToFloat(double d) {
        return (float) ((double) Math.round(d * 1.0E8D) / 1.0E8D);
    }
}