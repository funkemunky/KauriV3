package dev.brighten.ac.check.impl.combat.killaura;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

/**
 * @author funkemunky
 * @since 12/31/2020
 * This check is designed to detect an attack through solid blocks, which would be impossible under normal
 * circumstances.
 */
@CheckData(name = "KillAura (Trace)", checkId = "katrace", type = CheckType.KILLAURA, maxVersion = ClientVersion.V_1_21_5)
public class KATrace extends Check {

    public KATrace(APlayer player) {
        super(player);
    }

    private int buffer;

    @Bind
    WAction<WrapperPlayClientInteractEntity> useEntity = packet -> {
        if(player.getInfo().getTarget() == null
                || packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK)
            return;

        // If the player isn't looking at the target, then a raytrace check wouldn't work.
        if(player.getMovement().getLookingAtBoxes().isEmpty()) {
            debug("No boxes to look at!");
            buffer = 0;
            return;
        }

        var trackedEntity = player.getWorldTracker().getCurrentWorld().get().getTrackedEntity(player.getInfo().getTarget().getEntityId());

        if(trackedEntity.isEmpty()) {
            return;
        }
        // Getting the target's bounding box
        SimpleCollisionBox targetBox = (SimpleCollisionBox) EntityData.getEntityBox(player.getInfo().getTarget()
                        .getLocation(), trackedEntity.get());

        if(targetBox == null) return;

        final KLocation origin = player.getMovement().getTo().getLoc().clone();

        // Setting the player's eye height based on their sneak status
        origin.add(0, player.getEyeHeight(), 0);

        final Vector3d originVec = origin.toVector();

        // Setting a trace based on their view direction
        RayCollision collision = new RayCollision(originVec, origin.getDirection());

        Vector3d targetPoint = collision.collisionPoint(targetBox);
        //If the ray isn't collided, we might as well not run this check. Just a simple boxes on array check
        if(targetPoint == null) return;

        // The distance bteween the player's eye and the intersect point of the ray and the target's bounding box.
        // We don't do the square root to save on performance.
        double dist = originVec.distanceSquared(targetPoint);

        boolean rayCollidedOnBlock = false;

        // Grabbing the boxes found on the ray trace. This is already grabbed in MovementProcessor so we can use
        // the result from there in this check to save on compute time.
        synchronized (player.getMovement().getLookingAtBoxes()) {
            for (CollisionBox lookingAtBox : player.getMovement().getLookingAtBoxes()) {
                if((lookingAtBox instanceof SimpleCollisionBox box)) {
                    if(box.minX % 1 != 0 || box.minY % 1 != 0 || box.minZ % 1 != 0
                            || box.maxX % 1 != 0 || box.maxY % 1 != 0 || box.maxZ % 1 != 0)
                        continue;

                    // We want to shrink the box slightly since there is a bit of a margin of error in the ray trace.
                    Vector3d point = collision.collisionPoint(box.copy().shrink(0.005f, 0.005f, 0.005f));

                    if (point != null && originVec.distanceSquared(point) < dist - 0.2) {
                        rayCollidedOnBlock = true;
                        break;
                    }
                }
            }
        }

        if(rayCollidedOnBlock) {
            // The buffer is here to smooth out false positives.
            if(++buffer > 2) {
                flag("Attacker hit through block! [b=%s s=%s]",
                        buffer, player.getMovement().getLookingAtBoxes().size());
            }
        } else if(buffer > 0) buffer--;

        debug("b=%s collides=%s", buffer, rayCollidedOnBlock);
    };
}
