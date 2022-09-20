package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;

@CheckData(name = "Vertical", checkId = "verticala", type = CheckType.MOVEMENT)
public class Vertical extends Check {
    public Vertical(APlayer player) {
        super(player);
    }
}
