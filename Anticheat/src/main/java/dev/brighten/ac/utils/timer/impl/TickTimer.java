package dev.brighten.ac.utils.timer.impl;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.timer.Timer;

public class TickTimer implements Timer {
    private long currentStamp;
    private int resetStreak;
    private final long defaultPassed;

    public TickTimer(long defaultPassed) {
        this.defaultPassed = defaultPassed;
        currentStamp = Anticheat.INSTANCE.getKeepaliveProcessor().tick;
    }

    public TickTimer() {
        defaultPassed = 20;
    }

    @Override
    public boolean isPassed(long stamp) {
        return getPassed() > stamp;
    }

    @Override
    public boolean isPassed() {
        return getPassed() > defaultPassed;
    }

    @Override
    public boolean isNotPassed(long stamp) {
        return getPassed() <= stamp;
    }

    @Override
    public boolean isNotPassed() {
        return getPassed() <= defaultPassed;
    }

    @Override
    public boolean isReset() {
        return getPassed() == 0;
    }

    @Override
    public int getResetStreak() {
        return resetStreak;
    }

    @Override
    public long getPassed() {
        return Anticheat.INSTANCE.getKeepaliveProcessor().tick - currentStamp;
    }

    @Override
    public long getCurrent() {
        return currentStamp;
    }

    @Override
    public void reset() {
        if(getPassed() <= 1) resetStreak++;
        else resetStreak = 0;

        currentStamp = Anticheat.INSTANCE.getKeepaliveProcessor().tick;
    }
}
