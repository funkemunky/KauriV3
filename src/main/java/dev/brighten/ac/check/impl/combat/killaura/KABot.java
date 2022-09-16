package dev.brighten.ac.check.impl.combat.killaura;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import lombok.val;

@CheckData(name = "KillAura (Bot)", checkId = "kabot", type = CheckType.KILLAURA)
public class KABot extends Check {

    public KABot(APlayer player) {
        super(player);
    }

    private int buffer = 0;

    WAction<WPacketPlayInUseEntity> packet = packet -> {
        val optional = player.getEntityLocationHandler().getFakeMob(packet.getEntityId());

        if(optional.isPresent() && player.getEntityLocationHandler().clientHasEntity.get()) {
            if(++buffer > 3) {
                flag("Attacked player without attacking bot!");
            }
        } else buffer = 0;
    };
}
