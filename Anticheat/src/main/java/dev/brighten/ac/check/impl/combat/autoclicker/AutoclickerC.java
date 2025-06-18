package dev.brighten.ac.check.impl.combat.autoclicker;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingList;

import java.util.List;

@CheckData(name = "Autoclicker (C)", checkId = "autoclickerc", type = CheckType.AUTOCLICKER, maxVersion = ClientVersion.V_1_21_5)
public class AutoclickerC extends Check {

    public AutoclickerC(APlayer player) {
        super(player);
    }

    private int lastPlace;
    private float buffer;
    private final List<Integer> tickDeltas = new EvictingList<>(30);

    @Bind
    WAction<WrapperPlayClientAnimation> action = (packet) -> {
        if(player.getInfo().isBreakingBlock()
                || player.getWrappedPlayer().getItemInHand().getType().isBlock()
                || packet.getHand() != InteractionHand.MAIN_HAND) return;

        int currentTick = player.getPlayerTick();
        int deltaPlace = currentTick - lastPlace;

        tickDeltas.add(deltaPlace);
        if(tickDeltas.size() > 8) {
            int max = -10000000, min = Integer.MAX_VALUE;
            double average = 0;
            int range, total = 0;

            for (Integer delta : tickDeltas) {
                max = Math.max(delta, max);
                min = Math.min(delta, min);

                average+= delta;
                total++;
            }

            average/= total;
            range = max - min;

            if(average < 3 && range <= 1) {
                if(++buffer > 12) {
                    vl++;
                    flag("range=%s", range);
                }
            } else if(buffer > 0) buffer-= 0.5f;

            debug("range=%s average=%.1f b=%.1f", range, average, buffer);
        }

        debug("deltaArm=%s b=%.1f", deltaPlace, buffer);
        lastPlace = currentTick;
    };
}
