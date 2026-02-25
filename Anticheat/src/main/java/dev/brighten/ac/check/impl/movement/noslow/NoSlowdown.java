package dev.brighten.ac.check.impl.movement.noslow;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;

@CheckData(name = "NoSlow (Hand)", checkId = "noslowhand", type = CheckType.MOVEMENT, punishVl = 5, punishable = false, experimental = true)
public class NoSlowdown extends Check {
    public NoSlowdown(APlayer player) {
        super(player);
    }


}
