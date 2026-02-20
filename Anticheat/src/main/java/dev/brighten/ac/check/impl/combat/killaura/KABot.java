package dev.brighten.ac.check.impl.combat.killaura;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;
import lombok.val;

@CheckData(name = "KillAura (Bot)", checkId = "kabot", type = CheckType.KILLAURA)
public class KABot extends Check {

    public KABot(APlayer player) {
        super(player);
    }

    private int buffer = 0;
    private float buffer2;

    @Bind
    WAction<WrapperPlayClientAnimation> arm = packet -> {
        // We want to go ahead and lower the buffer every time they miss the bot to help prevent false positives
        if(buffer2 > 0) buffer2-= 0.25f;
    };

    @Bind
    WAction<WrapperPlayClientInteractEntity> packet = packet -> {
        val optional = player.getWorldTracker().getCurrentWorld().get().getTrackedEntity(packet.getEntityId());

        if(optional.isPresent()
                && (player.getEntityTrackHandler().clientHasEntity.get()
                || !optional.get().getFakeMobs().isEmpty()))  {
            if(++buffer > 3) {
                flag("Attacked player without attacking bot!");
            }
        } else buffer = 0;

        if(player.getMob().getEntityId() == packet.getEntityId()) {
            if(++buffer2 > 3) {
                buffer = 2;
                flag("Player attacked bot");
            }
        } else buffer2 = 0;
    };
}
