package dev.brighten.ac.check.impl.misc.inventory;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

@CheckData(name = "Inventory (FastClick)", checkId = "inventoryd", type = CheckType.INVENTORY)
public class InventoryD extends Check {
    public InventoryD(APlayer player) {
        super(player);
    }

    private long lastClick = 0, lastClickPT = 0;
    private float buffer = 0;

    WAction<InventoryClickEvent> windowClick = (event) -> {
        int playerTick = player.getPlayerTick();

        check: {
            if(event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || lastClick == 0 || lastClickPT == 0) {
                break check;
            }

            long delta = System.currentTimeMillis() - lastClick;
            if(delta < 50 && playerTick - lastClickPT <= 1) {
                if(++buffer > 5) {
                    flag("b=%.1f d=%s", buffer, delta);
                    buffer = Math.min(7, buffer); //Limiting buffer add
                }
            } else if(buffer > 0) buffer-= 0.25f;

            debug("b=%.1f d=%s pt=%s", buffer, delta, playerTick);
        }

        lastClick = System.currentTimeMillis();
        lastClickPT = playerTick;
    };
}
