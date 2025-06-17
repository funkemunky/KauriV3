package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.annotation.Bind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CheckData(name = "Fly (Height)", checkId = "flyb", type = CheckType.MOVEMENT)
public class FlyB extends Check {
    public FlyB(APlayer player) {
        super(player);
    }

    private double slimeY = 0;

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flyingAction = packet -> {
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

        if(player.getBlockInfo().onSlime && packet.isOnGround() && player.getMovement().getDeltaY() < 0) {
            double ldeltaY = player.getMovement().getLDeltaY() * -1, deltaY = player.getMovement().getDeltaY() * -1;

            if(ldeltaY > deltaY) {
                deltaY+= ldeltaY;
            }
            slimeY = deltaY;
            debug("SlimeY: " + slimeY);
        } else if(!player.getInfo().wasOnSlime)
            slimeY = 0;

        // Adding all possible velocity deltaY.
        player.getVelocityHandler().getPossibleVectors().forEach(vec -> possibleHeights.add(vec.getY()));

        if(player.getInfo().lastHalfBlock.isNotPassed(1)
                || player.getInfo().lastFence.isNotPassed(1) || player.getBlockInfo().fenceNear) {
            possibleHeights.add(0.5);
        }

        jumpCheck: {
            if(!jumped || player.getInfo().blockAbove.isNotPassed(1)
                    || player.getInfo().climbTimer.isNotPassed(1)
                    || player.getInfo().wasOnSlime
                    || player.getBlockInfo().nearSteppableEntity
                    || player.getInfo().lastFence.isNotPassed(1)
                    || player.getBlockInfo().fenceNear
                    || player.getInfo().lastHalfBlock.isNotPassed(1)
                    || player.getInfo().slimeTimer.isNotPassed(1)
                    || player.getInfo().lastLiquid.isNotPassed(1)) break jumpCheck;

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

        if(player.getInfo().wasOnSlime)
            possibleHeights.add(slimeY);

        maximumHeightCheck: {
            if(player.getInfo().nearGround || player.getBlockInfo().nearSteppableEntity) break maximumHeightCheck;

            double maxHeight = possibleHeights.stream().max(Comparator.comparing(c -> c)).orElse(1.5) + 0.05f;

            if(player.getMovement().getDeltaY() > maxHeight) {
                flag("%.4f>-%.4f", player.getMovement().getDeltaY(), maxHeight);
            }
        }
    };
}
