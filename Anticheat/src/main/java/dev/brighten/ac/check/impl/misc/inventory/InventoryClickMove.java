package dev.brighten.ac.check.impl.misc.inventory;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "Inventory (ClickMove)", checkId = "inventoryc", type = CheckType.INVENTORY, maxVersion = ClientVersion.V_1_21_5)
public class InventoryClickMove extends Check {
    public InventoryClickMove(APlayer player) {
        super(player);
    }

    private int lastWindowClick = -2;

    // Updating the last time the player clicked in a menu for use in the below check for positional movement.
    @Bind
    WAction<WrapperPlayClientClickWindow> windowClick = packet -> lastWindowClick = player.getPlayerTick();

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        // If the player isn't sending any rotational or positional updates, then we don't need to check them.
        if((!packet.hasPositionChanged()
                || player.getPlayerTick() < 10 // Can false flag on join
                || (player.getMovement().getDeltaX() == 0 && player.getMovement().getDeltaZ() == 0))
                && !packet.hasRotationChanged()) return;

        // The player is not moving, therefore we do not check when they last clicked since this behavior
        // is legitimate.
        if(player.EMULATOR.getInput() == null || player.EMULATOR.getInput().getForward() == 0 && player.EMULATOR.getInput().getStrafing() == 0)
            return;

        // Any of these could result in false positives as our emulator does not account for these things yet.
        if(player.getInfo().lastLiquid.isNotPassed(3)
                || player.getMovement().getLastTeleport().isNotPassed(2)
                || player.getInfo().climbTimer.isNotPassed(2)
                || player.getInfo().isDoingVelocity()
                || !player.getVelocityHandler().getPossibleVectors().isEmpty()
                || player.getInfo().velocity.isNotPassed(7)
                || player.getBlockInfo().pistonNear) {
            return;
        }

        final int CLICK_DELTA = player.getPlayerTick() - lastWindowClick;

        // If they recently clicked in an inventory while getting passed all of these previous checks, they are most
        // likely using an illegal module to allow them to move with an open menu.
        if(CLICK_DELTA <= 1) {
            flag("Clicked while moving [%s, %s]",
                    player.EMULATOR.getInput().getForward(), player.EMULATOR.getInput().getStrafing());
        }
    };
}
