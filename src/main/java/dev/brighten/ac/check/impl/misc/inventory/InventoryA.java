package dev.brighten.ac.check.impl.misc.inventory;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;

/**
 * Check runs inside {@link dev.brighten.ac.check.impl.movement.speed.Horizontal}
 */
@CheckData(name = "Inventory (A)", checkId = "inventoryA", type = CheckType.INVENTORY)
public class InventoryA extends Check {

    public InventoryA(APlayer player) {
        super(player);
    }

    public int buffer;
}
