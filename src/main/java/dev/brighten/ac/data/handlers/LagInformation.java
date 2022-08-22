package dev.brighten.ac.data.handlers;

import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LagInformation {
    private Timer lastPingDrop = new TickTimer(), lastPacketDrop = new TickTimer(), lastClientTransaction = new MillisTimer();
    private long transPing, lastTransPing, lastFlying;
}
