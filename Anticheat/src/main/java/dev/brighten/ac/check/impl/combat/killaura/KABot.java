package dev.brighten.ac.check.impl.combat.killaura;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInArmAnimation;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import lombok.val;

import java.util.List;

@CheckData(name = "KillAura (Bot)", checkId = "kabot", type = CheckType.KILLAURA)
public class KABot extends Check {

    public KABot(APlayer player) {
        super(player);
    }

    private int buffer = 0;
    private float buffer2;

    WAction<WPacketPlayInArmAnimation> arm =  packet -> {
        // We want to go ahead and lower the buffer every time they miss the bot to help prevent false positives
        if(buffer2 > 0) buffer2-= 0.25f;
    };

    WAction<WPacketPlayInFlying> flying = packet -> {
        if(player.getInfo().lastAttack.isNotPassed(20)) {
            player.getMob().setInvisible(false);
        } else player.getMob().setInvisible(true);
    };

    WAction<WPacketPlayInUseEntity> packet = packet -> {
        val optional = player.getEntityLocationHandler().getFakeMob(packet.getEntityId());

        if(optional.isPresent()
                && (player.getEntityLocationHandler().clientHasEntity.get()
                || player.getEntityLocationHandler()
                        .getFakeMob(player.getBukkitPlayer().getEntityId())
                        .map(List::size).orElse(0) > 0))  {
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
