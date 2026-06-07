package dev.brighten.ac.check.impl.misc;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.TransactionClientWrapper;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

@CheckData(name = "TransactionSpoof", checkId = "transactionspoof", type = CheckType.EXPLOIT)
public class TransactionSpoof extends Check {

    public TransactionSpoof(APlayer player) {
        super(player);
    }

    private int positionsSinceLastTrans = 0, keepAlivesSinceLastTrans = 0;
    private Timer lastPosition = new TickTimer();

    @Bind
    WAction<TransactionClientWrapper> clientTransaction = packet -> {
        positionsSinceLastTrans = 0;
        keepAlivesSinceLastTrans = 0;
    };

    @Bind
    WAction<WrapperPlayClientKeepAlive> clientKeepalive = packet -> {
        if(keepAlivesSinceLastTrans > 5 && positionsSinceLastTrans > 5) {
            vl++;
            flag("Too many keep alives since last transaction %s", keepAlivesSinceLastTrans);
        }
      keepAlivesSinceLastTrans++;

        debug("keepAlive=%s packetId=%s", keepAlivesSinceLastTrans, packet.getId());
    };

    @Bind
    WAction<WrapperPlayServerKeepAlive> serverKeepalive = packet -> {
        debug("[Server] keepAlive=%s packetId=%s", keepAlivesSinceLastTrans, packet.getId());
    };

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        positionsSinceLastTrans++;

        if(positionsSinceLastTrans > 10) {
            player.sendPacket(new WrapperPlayServerKeepAlive(player.getPlayerTick()));
        }

        if(positionsSinceLastTrans > 40) {
            vl++;
            flag("Too many positions since last transaction %s", positionsSinceLastTrans);
            if(vl > 10) {
                Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().kickPlayer("Connection Timed Out"));
            }
        }

        lastPosition.reset();
    };
}
