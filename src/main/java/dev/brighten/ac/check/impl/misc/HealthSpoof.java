package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityMetadata;

@CheckData(name = "HealthSpoof", checkId = "healthspoof", type = CheckType.EXPLOIT)
public class HealthSpoof extends Check {

    public HealthSpoof(APlayer player) {
        super(player);
    }

    WCancellable<WPacketPlayOutEntityMetadata> event = packet -> {
        if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) return false;

        packet.getWatchedObjects().forEach(obj -> {
            debug("%s: %s", obj.getDataValueId(), obj.getDataWatcherObject());
        });
        return false;
    };
}
