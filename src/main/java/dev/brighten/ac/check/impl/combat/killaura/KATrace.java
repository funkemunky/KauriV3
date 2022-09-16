package dev.brighten.ac.check.impl.combat.killaura;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.util.Vector;

@CheckData(name = "KillAura (Trace)", checkId = "katrace", type = CheckType.KILLAURA)
public class KATrace extends Check {

    private float sneakY = 1.54f;
    public KATrace(APlayer player) {
        super(player);

        sneakY = player.getPlayerVersion().isBelow(ProtocolVersion.V1_14) ? 1.27f : 1.54f;
    }

    private int buffer;

    WAction<WPacketPlayInUseEntity> useEntity = packet -> {
        if(player.getInfo().getTarget() == null
                || packet.getAction() != WPacketPlayInUseEntity.EnumEntityUseAction.ATTACK)
            return;

        if(player.getMovement().getLookingAtBoxes().size() == 0) {
            debug("No boxes to look at!");
            buffer = 0;
            return;
        }

        SimpleCollisionBox targetBox = (SimpleCollisionBox) EntityData.getEntityBox(player.getInfo().getTarget().getLocation(),
                player.getInfo().target);

        if(targetBox == null) return;

        KLocation origin = player.getMovement().getTo().getLoc().clone();

        origin.y+= player.getInfo().isSneaking() ? sneakY : 1.62f;

        RayCollision collision = new RayCollision(origin.toVector(), MathUtils.getDirection(origin));

        Vector targetPoint = collision.collisionPoint(targetBox);
        //If the ray isn't collided, we might as well not run this check. Just a simple boxes on array check
        if(targetPoint == null) return;

        double dist = origin.toVector().distanceSquared(targetPoint);

        boolean rayCollidedOnBlock = false;

        synchronized (player.getMovement().getLookingAtBoxes()) {
            for (CollisionBox lookingAtBox : player.getMovement().getLookingAtBoxes()) {
                if((lookingAtBox instanceof SimpleCollisionBox)) {
                    SimpleCollisionBox box = (SimpleCollisionBox) lookingAtBox;
                    if(box.xMin % 1 != 0 || box.yMin % 1 != 0 || box.zMin % 1 != 0
                            || box.xMax % 1 != 0 || box.yMax % 1 != 0 || box.zMax % 1 != 0)
                        continue;

                    Vector point = collision.collisionPoint(box.copy().shrink(0.005f, 0.005f, 0.005f));

                    if (point != null && origin.toVector().distanceSquared(point) < dist - 0.2) {
                        rayCollidedOnBlock = true;
                        break;
                    }
                }
            }
        }

        if(rayCollidedOnBlock) {
            if(++buffer > 2) {
                flag("Attacker hit through block! [b=%s s=%s]",
                        buffer, player.getMovement().getLookingAtBoxes().size());
            }
        } else if(buffer > 0) buffer--;

        debug("b=%s collides=%s", buffer, rayCollidedOnBlock);
    };
}
