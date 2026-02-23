package dev.brighten.ac.check.impl.combat.killaura;

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author funkemunky
 * @since 12/31/2020
 * This check is designed to detect an attack through solid blocks, which would be impossible under normal
 * circumstances.
 */
@CheckData(name = "KillAura (Trace)", checkId = "katrace", type = CheckType.KILLAURA)
public class KATrace extends Check {

    public KATrace(APlayer player) {
        super(player);
    }

    private int buffer;

    private final Queue<TrackedEntity> attacks = new LinkedList<>();

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        // If the player isn't looking at the target, then a raytrace check wouldn't work.
        synchronized (attacks) {
            TrackedEntity target;

            while((target = attacks.poll()) != null) runCheck(target);
        }
    };

    private void runCheck(TrackedEntity target) {
        if(player.getMovement().getLookingAtBoxes().isEmpty()) {
            debug("No boxes to look at!");
            buffer = 0;
            return;
        }
        if(target.getNewEntityLocation().x == 0 && target.getNewEntityLocation().y == 0 & target.getNewEntityLocation().z == 0) {
            return;
        }

        short collisions = 0;
        var required = target.getNewEntityLocation().interpolatedLocations.size();

        if(required == 0) return;

        for (KLocation targetLoc : target.getNewEntityLocation().interpolatedLocations) {
            // Getting the target's bounding box
            SimpleCollisionBox targetBox = (SimpleCollisionBox) EntityData.getEntityBox(targetLoc, target);

            final KLocation origin = player.getMovement().getTo().getLoc().clone();

            // Setting the player's eye height based on their sneak status
            origin.add(0, player.getEyeHeight(), 0);

            final Vector3d originVec = origin.toVector();

            // Setting a trace based on their view direction
            RayCollision collision = new RayCollision(originVec, origin.getDirection());

            Vector3d targetPoint = collision.collisionPoint(targetBox);
            //If the ray isn't collided, we might as well not run this check. Just a simple boxes on array check
            if(targetPoint == null) continue;

            // The distance bteween the player's eye and the intersect point of the ray and the target's bounding box.
            // We don't do the square root to save on performance.
            double dist = originVec.distanceSquared(targetPoint);

            // Grabbing the boxes found on the ray trace. This is already grabbed in MovementProcessor so we can use
            // the result from there in this check to save on compute time.
            synchronized (player.getMovement().getLookingAtBoxes()) {
                for (SimpleCollisionBox lookingAtBox : player.getMovement().getLookingAtBoxes()) {
                    //lookingAtBox.draw(ParticleTypes.FLAME, player);

                    var boxCollisionPoint = collision.collisionPoint(lookingAtBox);

                    if(boxCollisionPoint == null) continue;

                    if(originVec.distanceSquared(boxCollisionPoint) < dist) {
                        collisions++;
                        lookingAtBox.downCast().forEach(box -> debug(box.toString()));
                        break;
                    }
                }
            }
        }

        if(collisions >= required) {
            // The buffer is here to smooth out false positives.
            if(++buffer > 1) {
                flag("Attacker hit through block! [b=%s s=%s]",
                        buffer, player.getMovement().getLookingAtBoxes().size());
            }
        } else if(buffer > 0) buffer--;

        debug("b=%s collides=%s eyeHeight=%.2f", buffer, collisions, player.getEyeHeight());
    }

    @Bind
    WAction<WrapperPlayClientInteractEntity> useEntity = packet -> {
        if(player.getInfo().getTarget() == null
                || packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK)
            return;

        synchronized (attacks) {
            attacks.add(player.getInfo().getTarget());
        }
    };
}
