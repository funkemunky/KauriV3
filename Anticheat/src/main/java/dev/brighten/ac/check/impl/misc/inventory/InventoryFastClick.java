package dev.brighten.ac.check.impl.misc.inventory;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Inventory (FastClick)", checkId = "inventoryd", type = CheckType.INVENTORY, maxVersion = ClientVersion.V_1_21_5)
public class InventoryFastClick extends Check {
    public InventoryFastClick(APlayer player) {
        super(player);
    }

    private long lastClick = 0, lastClickPT = 0;
    private float buffer = 0;

    @Bind
    WAction<WrapperPlayClientClickWindow> windowClick = (event) -> {
        int playerTick = player.getPlayerTick();

        check: {
            if(event.getWindowClickType() != WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE
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
