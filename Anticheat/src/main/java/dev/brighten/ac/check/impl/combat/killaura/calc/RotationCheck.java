package dev.brighten.ac.check.impl.combat.killaura.calc;

import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.Tuple;

public interface RotationCheck {
    void runCheck(Tuple<EntityLocation, EntityLocation> locs, double[] std, double[] offset, float[] rot);
}
