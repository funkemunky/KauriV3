package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityVelocity;
import dev.brighten.ac.utils.Async;

@CheckData(name = "Fly (C)", checkId = "flyc", type = CheckType.MOVEMENT)
public class FlyC extends Check {
    public FlyC(APlayer player) {
        super(player);
    }

    @Async
    WAction<WPacketPlayInFlying> flyingAction = packet -> {
        boolean ground = player.getMovement().getTo().isOnGround(),
                fground = player.getMovement().getFrom().isOnGround();
    };
}
