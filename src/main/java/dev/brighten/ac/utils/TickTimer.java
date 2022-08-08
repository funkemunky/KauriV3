package dev.brighten.ac.utils;


import dev.brighten.ac.Anticheat;

public class TickTimer {
    private int ticks = Anticheat.INSTANCE.getCurrentTicks(), defaultPassed;

    public TickTimer(int defaultPassed) {
        this.defaultPassed = defaultPassed;
    }

    public void reset() {
        ticks = Anticheat.INSTANCE.getCurrentTicks();
    }

    public boolean hasPassed() {
        return Anticheat.INSTANCE.getCurrentTicks() - ticks > defaultPassed;
    }

    public boolean hasPassed(int amount) {
        return Anticheat.INSTANCE.getCurrentTicks() - ticks > amount;
    }

    public boolean hasNotPassed() {
        return Anticheat.INSTANCE.getCurrentTicks() - ticks <= defaultPassed;
    }

    public boolean hasNotPassed(int amount) {
        return Anticheat.INSTANCE.getCurrentTicks() - ticks <= amount;
    }

    public int getPassed() {
        return Anticheat.INSTANCE.getCurrentTicks() - ticks;
    }
}
