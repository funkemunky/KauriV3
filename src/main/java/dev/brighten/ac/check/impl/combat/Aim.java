package dev.brighten.ac.check.impl.combat;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;

import java.util.List;

@CheckData(name = "Aim", type = CheckType.COMBAT)
public class Aim extends Check {
    public Aim(APlayer player) {
        super(player);
    }

    private float buffer;
    protected Timer lastGrid = new TickTimer(3);

    @Action
    public void flying(WPacketPlayInFlying packet) {
        if(!packet.isLooked()) return;

        if(getPlayer().getMovement().getYawGcdList().size() < 40
                || getPlayer().getMovement().getLookX() > 200 || getPlayer().getMovement().getLookY() > 200) {
            if(buffer > 0) buffer--;
            return;
        }

        if(getPlayer().getMovement().getPitchGCD() < 0.007
                && !getPlayer().getMovement().isCinematic()) {
            if(Math.abs(getPlayer().getMovement().getDeltaPitch()) < 10 && ++buffer > 4) {
                flag("gcd=%.5f", getPlayer().getMovement().getPitchGCD());
            }
        } else buffer = 0;

        debug("b=%s gcd=%.4f cin=%s", buffer, getPlayer().getMovement().getPitchGCD(),
                getPlayer().getMovement().isCinematic());

    }

    /*
     * This is an attempt to reverse the logistics of cinematic camera without having to run a full on prediction using
     * mouse filters. Otherwise, we would need to run more heavy calculations which is not really production friendly.
     * It may be more accurate but it is not really worth it if in the end of the day we're eating server performance.
     */
    protected static double getGrid(final List<Float> entry) {
        /*
         * We're creating the variables average min and max to start calculating the possibility of cinematic camera.
         * Why does this work? Cinematic camera is essentially a slowly increasing slowdown (which is why cinematic camera
         * becomes slower the more you use it) which in turn makes it so the min max and average are extremely close together.
         */
        double average = 0.0;
        double min = 0.0, max = 0.0;

        /*
         * These are simple min max calculations done manually for the sake of simplicity. We're using the numbers 0.0
         * since we also want to account for the possibility of a negative number. If there are no negative numbers then
         * there is absolutely no need for us to care about that number other than getting the max.
         */
        for (final double number : entry) {
            if (number < min) min = number;
            if (number > max) max = number;

            /*
             * Instead of having a sum variable we can use an average variable which we divide
             * right after the loop is over. Smart programming trick if you want to use it.
             */
            average += number;
        }

        /*
         * We're dividing the average by the length since this is the formula to getting the average.
         * Specifically its (sum(n) / length(n)) = average(n) -- with n being the entry set we're analyzing.
         */
        average /= entry.size();

        /*
         * This is going to estimate how close the average and the max were together with the possibility of a min
         * variable which is going to represent a negative variable since the preset variable on min is 0.0.
         */
        return (max - average) - min;
    }
}
