package dev.brighten.ac.check.impl.velocity;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import org.bukkit.util.Vector;

@CheckData(name = "Velocity (A)", type = CheckType.MOVEMENT)
public class VelocityA extends Check {

    private Vector currentVelocity = null;
    private float buffer = 0;

    public VelocityA(APlayer player) {
        super(player);

        player.onVelocity(velocity -> {
            currentVelocity = velocity.clone();
            debug("did velocity: " + currentVelocity.getY());
        });
    }

    Action<WPacketPlayInFlying> flying = packet -> {
        if(currentVelocity != null && currentVelocity.getY() > 0
                && !player.getBlockInfo().inWeb
                && !player.getBlockInfo().onClimbable
                && player.getInfo().getBlockAbove().isPassed(6)
                && !player.getBlockInfo().onSlime
                && !player.getInfo().isGeneralCancel()) {
            double pct = player.getMovement().getDeltaY() / currentVelocity.getY() * 100;

            if(currentVelocity.getY() < 0.005
                    || player.getBlockInfo().collidesHorizontally
                    || player.getInfo().getLastAbilities().isNotPassed(3)
                    || player.getBlockInfo().collidesVertically
                    || player.getInfo().getVelocity().isPassed(7)) {
                currentVelocity = null;
                return;
            }


            if(pct < 99.999 || pct > 400) {
                if(++buffer > 15) {
                    flag("pct=%.1f%% buffer=%.1f", pct, buffer);
                }
            } else if(buffer > 0) buffer-= 0.5;

            debug("pct=%.1f%% buffer=%.1f dy=%.4f vy=%.4f", pct, buffer,
                    player.getMovement().getDeltaY(), currentVelocity.getY());

            currentVelocity.setY((currentVelocity.getY() - 0.08) * 0.98);
        } else if(currentVelocity != null) {
            debug("not null: " + currentVelocity.getY());
        }
    };
}
