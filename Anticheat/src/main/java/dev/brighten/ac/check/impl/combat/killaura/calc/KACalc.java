package dev.brighten.ac.check.impl.combat.killaura.calc;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.check.Hook;
import dev.brighten.ac.check.KListener;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.impl.combat.killaura.calc.impl.KAGrid;
import dev.brighten.ac.check.impl.combat.killaura.calc.impl.KAZero;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.List;
import java.util.Optional;

@Hook
public class KACalc extends KListener {
    public KACalc(APlayer player) {
        super(player);
    }

    private final List<Double> YAW_OFFSET = new EvictingList<>(10),
            PITCH_OFFSET = new EvictingList<>(10);

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(player.getInfo().getTarget() == null || player.getInfo().lastAttack.isPassed(40)) return;
        Optional<TrackedEntity> optional = player.getWorldTracker().getCurrentWorld().get()
                .getTrackedEntity(player.getInfo().getTarget().getEntityId());

        if(optional.isEmpty()) return;

        EntityLocation location = optional.get().getNewEntityLocation();

        if(location == null) {
            return;
        }
        Vector3d current = location.getCurrentIteration();

        KLocation originKLoc = player.getMovement().getTo().getLoc().clone()
                .add(0, player.getInfo().isSneaking() ? 1.54f : 1.62f, 0);

        double halfHeight = player.getInfo().getTarget().getPose().eyeHeight / 2;
        KLocation targetLocation = new KLocation(current.getX(), current.getY(), current.getZ());
        var rotations = MathUtils
                .getRotation(originKLoc, targetLocation.clone().add(0, halfHeight, 0));
        var offset = MathUtils.getOffsetFromLocation(originKLoc, targetLocation);

        YAW_OFFSET.add(offset[0]);
        PITCH_OFFSET.add(offset[1]);

        if(YAW_OFFSET.size() < 10 || PITCH_OFFSET.size() < 10) return;

        double[] std = new double[2];

        std[0] = MathUtils.stdev(YAW_OFFSET);
        std[1] = MathUtils.stdev(PITCH_OFFSET);

        var elocTuple = new Tuple<>(optional.get().getNewEntityLocation(), optional.get().getOldEntityLocation());
        find(KAZero.class).ifPresent(zero -> zero.runCheck(elocTuple, std, offset, rotations));
        find(KAGrid.class).ifPresent(grid -> grid.runCheck(elocTuple, std, offset, rotations));
    };
}
