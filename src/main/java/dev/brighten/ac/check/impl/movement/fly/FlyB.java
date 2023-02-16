package dev.brighten.ac.check.impl.movement.fly;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;

@CheckData(name = "Fly (B)", checkId = "flyb", type = CheckType.MOVEMENT)
public class FlyB extends Check {
    public FlyB(APlayer player) {
        super(player);
    }


    private double lastGroundY;


    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isMoved())
            return;



        if(packet.isOnGround()) {
            lastGroundY = packet.getY();
        }
    };
}
