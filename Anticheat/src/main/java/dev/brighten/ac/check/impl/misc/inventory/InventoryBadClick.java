package dev.brighten.ac.check.impl.misc.inventory;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Inventory (BadClick)", checkId = "inventoryB", type = CheckType.INVENTORY, maxVersion = ClientVersion.V_1_11)
public class InventoryBadClick extends Check {

    public InventoryBadClick(APlayer player) {
        super(player);
    }

    @Bind
    WAction<WrapperPlayClientClickWindow> windowClick = packet -> {
        if(!player.getInfo().isInventoryOpen()) {
            flag("Inventory not open");
        }
        debug("inv=%s", player.getInfo().isInventoryOpen());
    };

}
