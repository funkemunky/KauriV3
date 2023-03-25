package dev.brighten.ac.check.impl.combat.killaura.calc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;

@CheckData(name = "KillAura (Calc)", checkId = "kacalc", type = CheckType.KILLAURA)
public class KACalc extends Check {
    public KACalc(APlayer player) {
        super(player);
    }

    List<float[]> floats = new EvictingList<>(100);

    @Bind
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(player.getInfo().getTarget() == null || player.getInfo().lastAttack.isPassed(40)) return;
        Optional<Tuple<EntityLocation, EntityLocation>> optional = player.getEntityLocationHandler()
                .getEntityLocation(player.getInfo().getTarget());

        if(optional.isEmpty()) return;

        Tuple<EntityLocation, EntityLocation> tuple = optional.get();

        EntityLocation location = tuple.one;
        Vector current = location.getCurrentIteration();

        KLocation originKLoc = player.getMovement().getTo().getLoc().clone()
                .add(0, player.getInfo().isSneaking() ? 1.54f : 1.62f, 0);

        double halfHeight = player.getInfo().getTarget().getEyeHeight() / 2;
        var rotations = MathUtils
                .getRotation(originKLoc, new KLocation(current.getX(), current.getY() + halfHeight, current.getZ()));

        var diff = new float[3];
        diff[0] = MathUtils.getAngleDelta(player.getMovement().getTo().getYaw(), rotations[0]);
        diff[1] = player.getMovement().getTo().getPitch() - rotations[1];
        diff[2] = player.getMovement().getTo().getYaw();

        floats.add(diff);

        if(floats.size() == 60) {
            float[] xFloats = new float[floats.size()], yawFloats = new float[floats.size()];

            for (int i = 0; i < floats.size(); i++) {
                float[] floats = this.floats.get(i);

                xFloats[i] = floats[0] + Math.abs(floats[1]);
                yawFloats[i] = floats[2];
            }

            double x = MathUtils.getAverage(xFloats), y = MathUtils.getGrid(yawFloats);

            debug("avg=%.4f grid=%.4f", x, y);
            floats.clear();
        }
    };
}
