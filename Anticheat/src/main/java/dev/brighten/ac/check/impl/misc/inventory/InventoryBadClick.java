package dev.brighten.ac.check.impl.misc.inventory;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInWindowClick;

@CheckData(name = "Inventory (BadClick)", checkId = "inventoryB", type = CheckType.INVENTORY, maxVersion = ProtocolVersion.V1_11)
public class InventoryBadClick extends Check {

    public InventoryBadClick(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInWindowClick> windowClick = packet -> {
        if(!player.getInfo().isInventoryOpen()) {
            flag("Inventory not open");
        }
        debug("inv=%s", player.getInfo().isInventoryOpen());
    };

}
