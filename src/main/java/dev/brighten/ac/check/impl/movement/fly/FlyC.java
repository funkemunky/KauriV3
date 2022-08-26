package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CheckData(name = "Fly (C)", checkId = "flyc", type = CheckType.MOVEMENT)
public class FlyC extends Check {
    public FlyC(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flyingAction = packet -> {
        if(player.getMovement().getLastTeleport().isNotPassed((1))
                || player.getMovement().getMoveTicks() <= 2
                || player.getInfo().isGeneralCancel())
            return;
        final boolean ground = player.getMovement().getTo().isOnGround(),
                fground = player.getMovement().getFrom().isOnGround();
        final boolean jumped = fground && !ground && player.getMovement().getDeltaY() > 0;

        final List<Double> possibleHeights = new ArrayList<>();

        // Adding only possible jump height with current circumstances
        possibleHeights.add(MovementUtils.getJumpHeight(player));

        // Adding all possible velocity deltaY.
        player.getVelocityHandler().getPossibleVectors().forEach(vec -> possibleHeights.add(vec.getY()));

        jumpCheck: {
            if(!jumped) break jumpCheck;

            // We want to check all possible heights
            for (Double possibleHeight : possibleHeights) {
                double delta = MathUtils.getDelta(player.getMovement().getDeltaY(), possibleHeight);

                if(delta < (player.getInfo().getLastBlockPlace().isNotPassed(20) ? 0.02 : 0.005)) {
                    debug("Found delta: dy=%.5f, p=%.5f", player.getMovement().getDeltaY(), possibleHeight);
                    break jumpCheck;
                }
            }

            // If we reach this point, it means no correct predicted deltaY was found
            flag("dy=%.5f p[%s]", player.getMovement().getDeltaY(), possibleHeights.stream()
                    .map(s -> String.valueOf(MathUtils.round(s, 5))).collect(Collectors.joining(";")));
        }

        maximumHeightCheck: {
            if(player.getInfo().nearGround) break maximumHeightCheck;

            double maxHeight = possibleHeights.stream().max(Comparator.comparing(c -> c)).orElse(1.5) + 0.05f;

            if(player.getMovement().getDeltaY() > maxHeight) {
                flag("%.4f>-%.4f", player.getMovement().getDeltaY(), maxHeight);
            }
        }
    };
}
