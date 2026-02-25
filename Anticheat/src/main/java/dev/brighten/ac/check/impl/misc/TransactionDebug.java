package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.TransactionClientWrapper;
import dev.brighten.ac.packet.TransactionServerWrapper;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "TransactionDebug", checkId = "transactiondebug", type = CheckType.EXPLOIT)
public class TransactionDebug extends Check {

    public TransactionDebug(APlayer player) {
        super(player);
    }

    @Bind
    WAction<TransactionClientWrapper> clientTrans = packet -> {
        debug("[%s, %s] Client", packet.getId(), packet.getAction());
    };

    @Bind
    WAction<TransactionServerWrapper> serverTrans = packet -> {
        debug("[%s, %s] Server", packet.getId(), packet.getAction());
    };
}
