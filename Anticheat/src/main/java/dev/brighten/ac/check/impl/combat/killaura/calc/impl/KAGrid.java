package dev.brighten.ac.check.impl.combat.killaura.calc.impl;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.impl.combat.killaura.calc.RotationCheck;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.objects.evicting.EvictingList;

import java.util.List;

@CheckData(name = "KillAura (Grid)", checkId = "kacalcgrid", type = CheckType.KILLAURA)
public class KAGrid extends Check implements RotationCheck {
    public KAGrid(APlayer player) {
        super(player);
    }

    private final List<Double> offsetGrid = new EvictingList<>(100);

    @Override
    public void runCheck(Tuple<EntityLocation, EntityLocation> locs, double[] std, double[] offset, float[] rot) {
        double totalOffset = Math.abs(offset[0]) + Math.abs(offset[1]);

        offsetGrid.add(totalOffset);

        if(offsetGrid.size() >= 50) {
            double grid = MathUtils.getGridDouble(offsetGrid);

            debug("grid=%.2f", grid);
        }
    }
}
