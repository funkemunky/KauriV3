package dev.brighten.ac.check.impl.combat.autoclicker;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.math.cond.MaxDouble;

@CheckData(name = "Autoclicker (D)", checkId = "autoclickerd", type = CheckType.AUTOCLICKER, punishVl = 15,
        maxVersion = ClientVersion.V_1_8)
public class AutoClickerD extends Check {
    public AutoClickerD(APlayer player) {
        super(player);
    }

    private long lastArm;
    private double cps;
    private boolean blocked;
    private int armTicks;
    private final MaxDouble verbose = new MaxDouble(40);

    @Bind
    WTimedAction<WrapperPlayClientAnimation> animation = (packet, timeStamp) -> {
        if(player.getInfo().isBreakingBlock()) return;

        cps = 1000D / (timeStamp - lastArm);
        lastArm = timeStamp;
        armTicks++;
    };

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(blocked) {
            if(armTicks > 0) {
                if(armTicks == 1 && cps > 3) {
                    if(cps > 7) verbose.add();
                    if(verbose.value() > 15) {
                        flag("arm=%s cps=%.3f lagging=%s", armTicks,
                                cps, player.getLagInfo());
                    }
                } else verbose.subtract(20);
                debug("cps=%s arm=%s vb=%s", cps, armTicks, verbose.value());
            }
            blocked = false;
            armTicks = 0;
        }
    };

    WAction<WrapperPlayClientPlayerBlockPlacement> place = packet -> {
        if(packet.getItemStack().isEmpty()
                || !packet.getItemStack().get().getType().hasAttribute(ItemTypes.ItemAttribute.SWORD))
            return;

        blocked = true;
    };
}
