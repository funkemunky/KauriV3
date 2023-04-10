package dev.brighten.ac.check.impl.combat.killaura.calc.impl;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.impl.combat.killaura.calc.RotationCheck;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.Tuple;

@CheckData(name = "KillAura (Zero)", checkId = "kacalczero", type = CheckType.KILLAURA)
public class KAZero extends Check implements RotationCheck {
    public KAZero(APlayer player) {
        super(player);
    }

    private int buffer;

    @Override
    public void runCheck(Tuple<EntityLocation, EntityLocation> locs, double[] std, double[] offset, float[] rot) {
        if(offset[0] == 0) {
            if(++buffer > 2) {
                flag("y=%.2f dy=.3f", offset[1], player.getMovement().getDeltaYaw());
            }
        } else buffer = 0;

        debug("offset=%s", offset[0]);
    }
}
