package dev.brighten.ac.check.impl.combat.autoclicker;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInArmAnimation;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "AutoClicker (A)", checkId = "autoclickera", type = CheckType.AUTOCLICKER, maxVersion = ProtocolVersion.V1_8_9)
public class AutoclickerA extends Check {
    public AutoclickerA(APlayer player) {
        super(player);
    }

    private int flyingTicks, cps;

    @Bind
    WAction<WPacketPlayInFlying> flying = (packet) -> {
        flyingTicks++;
        if(flyingTicks >= 20) {
            if(cps > 22) {
                if(cps > 30) {
                    punish();
                }
                flag("cps=%s", cps);
            }
            cps = 0;
        }
    };

    @Bind
    WAction<WPacketPlayInArmAnimation> armAnimation = packet -> {
        if(!player.getInfo().breakingBlock
                && player.getInfo().getLastBlockDig().isPassed(1)
                && player.getInfo().getLastBlockPlace().isPassed(1)) {
            cps++;
        }
        debug("breaking=%s", player.getInfo().breakingBlock);
    };
}
