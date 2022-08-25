package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityVelocity;

public class FlyC extends Check {
    public FlyC(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flyingAction = packet -> {
        boolean ground =
    };
}
