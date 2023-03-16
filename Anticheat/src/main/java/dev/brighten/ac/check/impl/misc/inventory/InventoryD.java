package dev.brighten.ac.check.impl.misc.inventory;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInWindowClick;

@CheckData(name = "Inventory (FastClick)", checkId = "inventoryd", type = CheckType.INVENTORY)
public class InventoryD extends Check {
    public InventoryD(APlayer player) {
        super(player);
    }

    private long lastClick = 0, lastClickPT = 0;
    private int lastSlot;
    private float buffer = 0;

    WTimedAction<WPacketPlayInWindowClick> windowClick = (packet, time) -> {
        int playerTick = player.getPlayerTick();

        check: {
            if(lastClick == 0 || lastClickPT == 0 || lastSlot == packet.getSlot()) break check;

            long delta = time - lastClick;
            if(delta < 50 && playerTick - lastClickPT <= 1) {
                if(++buffer > 5) {
                    flag("b=%.1f d=%s", buffer, delta);
                    buffer = Math.min(7, buffer); //Limiting buffer add
                }
            } else if(buffer > 0) buffer-= 0.25f;

            debug("b=%.1f d=%s pt=%s", buffer, delta, playerTick);
        }

        lastClick = time;
        lastClickPT = playerTick;
        lastSlot = packet.getSlot();
    };
}
