package dev.brighten.ac.check.impl.packet.badpackets;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.timer.impl.TickTimer;

@CheckData(name = "Timer", checkId = "timer", type = CheckType.ORDER)
public class Timer extends Check {

    public Timer(APlayer player) {
        super(player);
    }

    private int buffer;
    private final dev.brighten.ac.utils.timer.Timer lastFlag = new TickTimer();
    private long totalTimer = -1;

    @Bind
    WAction<WrapperPlayServerPlayerPositionAndLook> position = packet -> totalTimer-= 50;

    @Bind
    WAction<WrapperPlayClientPlayerBlockPlacement> blockPlace = packet -> {
        if(player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_17))
            totalTimer-= 50;
    };

    /**
     * Fixing bug with 1.9 since flying packets are not always sent
     */
    @Bind
    WTimedAction<WrapperPlayClientPong> transaction = (packet, timestamp) -> {
        if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_9)) return;

        Anticheat.INSTANCE.getKeepaliveProcessor().getKeepById((short)packet.getId()).ifPresent(ka -> {
            long delta = timestamp - ka.startStamp;

            if(delta < 1095L && totalTimer - (timestamp + 100) > 3000L) {
                totalTimer = timestamp - 300;
            }
        });
    };

    @Bind
    WTimedAction<WrapperPlayClientPlayerFlying> flying = (packet, timestamp) -> {
        if(totalTimer == -1) {
            totalTimer = player.getCreation().getCurrent() - 50;
            debug("set base time");
        }
        else totalTimer+= 50;

        long threshold = timestamp + 100, delta = totalTimer - threshold;

        boolean isLagProblem = (double)Anticheat.INSTANCE.getKeepaliveProcessor().laggyPlayers
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
