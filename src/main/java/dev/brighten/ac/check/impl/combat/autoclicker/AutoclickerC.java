package dev.brighten.ac.check.impl.combat.autoclicker;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInArmAnimation;
import dev.brighten.ac.utils.annotation.Async;

@CheckData(name = "Autoclicker (C)", checkId = "autoclickerc", type = CheckType.AUTOCLICKER)
public class AutoclickerC extends Check {

    public AutoclickerC(APlayer player) {
        super(player);
    }

    private int clicks;
    private long lastArm, totalTime;

    @Async
    WTimedAction<WPacketPlayInArmAnimation> action = (packet, now) -> {
        if(player.getInfo().isBreakingBlock()) return;

        long delta = now - lastArm;
        clicks++;

        totalTime+= delta;

        if(totalTime > 990) {
            if(clicks >= 3 && delta <= 200) {

            }
            clicks = 1;
            totalTime = 0;
        }

        lastArm = now;
    };
}
