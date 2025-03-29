package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityMetadata;
import dev.brighten.ac.utils.annotation.Bind;

@CheckData(name = "HealthSpoof", checkId = "healthspoof", type = CheckType.EXPLOIT, maxVersion = ProtocolVersion.V1_21_4)
public class HealthSpoof extends Check {

    public HealthSpoof(APlayer player) {
        super(player);
    }

    @Bind
    WCancellable<WPacketPlayOutEntityMetadata> event = packet -> {
        if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) return false;

        for (WrappedWatchableObject watchedObject : packet.getWatchedObjects()) {
            if (watchedObject.getDataValueId() == 6 && watchedObject.getWatchedObject() instanceof Float) {
                watchedObject.setWatchedObject(1f);

                HandlerAbstract.getHandler().sendPacketSilently(player, packet.getPacket());
                return true;
            }
        }
        return false;
    };
}
