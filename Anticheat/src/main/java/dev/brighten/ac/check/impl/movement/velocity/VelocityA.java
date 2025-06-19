package dev.brighten.ac.check.impl.movement.velocity;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;
import org.bukkit.util.Vector;

@CheckData(name = "Velocity (Vertical)", checkId = "velocitya", type = CheckType.MOVEMENT, maxVersion = ClientVersion.V_1_21_5)
public class VelocityA extends Check {

    private Vector currentVelocity = null;
    private float buffer = 0;

    public VelocityA(APlayer player) {
        super(player);

        player.onVelocity(velocity -> {
            currentVelocity = new Vector(velocity.getX(), velocity.getY(), velocity.getZ());
            debug("did velocity: " + currentVelocity.getY());
        });
    }

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
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
            } else if(buffer > 0) buffer-= 0.5F;

            debug("pct=%.1f%% buffer=%.1f dy=%.4f vy=%.4f", pct, buffer,
                    player.getMovement().getDeltaY(), currentVelocity.getY());

            currentVelocity.setY((currentVelocity.getY() - 0.08) * 0.98); // Reduce the velocity over time
        } else if(currentVelocity != null) {
            debug("not null: " + currentVelocity.getY());
        }
    };
}
