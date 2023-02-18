package dev.brighten.ac.handler.keepalive.actions.types;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.keepalive.actions.Action;
import dev.brighten.ac.handler.keepalive.actions.ActionType;
import lombok.Data;

@Data
public abstract class TransactionAction implements Action {
    private final APlayer player;
    private boolean confirmed = false;

    public TransactionAction(APlayer player) {
        this.player = player;
        player.runKeepaliveAction(ka -> {
            confirmed = true;
            run();
            Anticheat.INSTANCE.getActionManager().confirmedAction(this);
        });
    }

    @Override
    public abstract void run();

    @Override
    public boolean confirmed() {
        return confirmed;
    }

    @Override
    public ActionType type() {
        return ActionType.TRASNSACTION;
    }
}
