package dev.brighten.ac.check.impl.order;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.TimedWAction;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutPosition;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import net.minecraft.server.v1_8_R3.PacketPlayInTransaction;

@CheckData(name = "Timer", type = CheckType.ORDER)
public class Timer extends Check {

    public Timer(APlayer player) {
        super(player);
    }

    private int buffer;
    private final dev.brighten.ac.utils.timer.Timer lastFlag = new TickTimer(),
            lastReset = new TickTimer();
    private long totalTimer = -1;

    WAction<WPacketPlayOutPosition> position = packet -> {
        totalTimer-= 50;
    };

    WAction<WPacketPlayInBlockPlace> blockPlace = packet -> {
        if(player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_17))
            totalTimer-= 50;
    };

    /**
     * Fixing bug with 1.9 since flying packets are not always sent
     */
    TimedWAction<PacketPlayInTransaction> transaction = (packet, timestamp) -> {
        if(player.getPlayerVersion().isBelow(ProtocolVersion.V1_9)) return;

        Anticheat.INSTANCE.getKeepaliveProcessor().getKeepById(packet.b()).ifPresent(ka -> {
            long delta = timestamp - ka.startStamp;

            if(delta < 1095L && totalTimer - (timestamp + 100) > 3000L) {
                totalTimer = timestamp - 300;
            }
        });
    };

    TimedWAction<WPacketPlayInFlying> flying = (packet, timestamp) -> {
        if(totalTimer == -1) {
            totalTimer = player.getCreation().getCurrent() - 50;
            debug("set base time");
        }
        else totalTimer+= 50;

        long threshold = timestamp + 100, delta = totalTimer - threshold;

        boolean isLagProblem = Anticheat.INSTANCE.getKeepaliveProcessor().laggyPlayers
                / (double)Anticheat.INSTANCE.getKeepaliveProcessor().totalPlayers > 0.8;

        if(totalTimer > threshold && !isLagProblem) {
            if(++buffer > 5) {
                flag("p=%s;d=%s;r=%s", totalTimer, delta, isLagProblem);
            }
            totalTimer = timestamp - 80;
            debug("Flagged;reset time");
            lastFlag.reset();
        } else if(lastFlag.isPassed(100)) buffer = 0;

        debug("buffer=%s delta=%s threshold=%s ilp=%s", buffer, delta, threshold, isLagProblem);
    };
}
