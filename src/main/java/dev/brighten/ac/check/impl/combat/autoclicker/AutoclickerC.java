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

    private long totalClickTime, lastClickTime;
    private int clicks, oscillationTime, oscillationLevel, lowest, highest;

    @Async
    WTimedAction<WPacketPlayInArmAnimation> action = (packet, timeStamp) -> {
        if(player.getInfo().isBreakingBlock()) return;

        clicks++;
        long diff = timeStamp - lastClickTime;

        if ((totalClickTime += diff) > 990) {

            if (clicks >= 3 && diff <= 200.0f) {
                int time = oscillationTime + 1;
                int lowest = this.lowest;
                int highest = this.highest;

                if (lowest == -1) {
                    lowest = clicks;
                } else if (clicks < lowest) {
                    lowest = clicks;
                }
                if (highest == -1) {
                    highest = clicks;
                } else if (clicks > highest) {
                    highest = clicks;
                }

                int oscillation = highest - lowest;
                int oscLevel = oscillationLevel;
                if (time >= 9) {
                    if (highest >= 8) {
                        if (highest >= 9 && oscillation <= 5) {
                            oscLevel += 2;
                        }
                        if (oscillation > 3 && oscLevel > 0) {
                            --oscLevel;
                        }
                    } else if (oscLevel > 0) {
                        --oscLevel;
                    }
                    time = 0;
                    highest = -1;
                    lowest = -1;
                }
                if (oscillation > 2) {
                    time = 0;
                    oscLevel = 0;
                    highest = -1;
                    lowest = -1;
                }
                if (oscLevel >= 10) {
                    vl++;
                    flag("osc=" + oscLevel);
                }
                debug("osc=%s level=%s high=%s low=%s", oscillation, oscLevel, highest, lowest);
                this.lowest = lowest;
                this.highest = highest;
                this.oscillationTime = time;
                this.oscillationLevel = oscLevel;

            }
            totalClickTime = 0;
            clicks = 1;
        }
        lastClickTime = timeStamp;
    };
}
