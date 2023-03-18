package dev.brighten.ac.check.impl.combat.autoclicker;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInArmAnimation;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.math.cond.MaxDouble;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;

@CheckData(name = "Autoclicker (D)", checkId = "autoclickerd", type = CheckType.AUTOCLICKER, punishVl = 15,
        maxVersion = ProtocolVersion.V1_8_9)
public class AutoclickerD extends Check {
    public AutoclickerD(APlayer player) {
        super(player);
    }

    private static final EnumSet<Material> SWORDS = EnumSet.allOf(Material.class);

    static {
        Arrays.stream(Material.values()).filter(material -> material.name().contains("SWORD"))
                .forEach(SWORDS::add);
    }

    private long lastArm;
    private double cps;
    private boolean blocked;
    private int armTicks;
    private final MaxDouble verbose = new MaxDouble(40);

    WTimedAction<WPacketPlayInArmAnimation> animation = (packet, timeStamp) -> {
        if(player.getInfo().isBreakingBlock()) return;

        cps = 1000D / (timeStamp - lastArm);
        lastArm = timeStamp;
        armTicks++;
    };

    WAction<WPacketPlayInFlying> flying = packet -> {
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

    WAction<WPacketPlayInBlockPlace> place = packet -> {
        if(packet.getItemStack() == null || !SWORDS.contains(packet.getItemStack().getType())) return;
        blocked = true;
    };
}
