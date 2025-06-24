package dev.brighten.ac.check.impl.misc;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;

@CheckData(name = "HealthSpoof", checkId = "healthspoof", type = CheckType.EXPLOIT, maxVersion = ClientVersion.V_1_21_5)
public class HealthSpoof extends Check {

    public HealthSpoof(APlayer player) {
        super(player);
    }

    /*@Bind
    WCancellable<WrapperPlayServerEntityMetadata> event = packet -> {
        if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) return false;

        List<EntityData<?>> newEntityMetadata = new ArrayList<>();

        boolean overrwrite = false;
        for (EntityData<?> data : packet.getEntityMetadata()) {
            if(data.getType() == EntityDataTypes.FLOAT && data.getIndex() == 6) {
                newEntityMetadata.add(new EntityData<>(6, EntityDataTypes.FLOAT, 1f));
                overrwrite = true;
            } else {
                newEntityMetadata.add(data);
            }
        }

        if(overrwrite) {
            WrapperPlayServerEntityMetadata newPacket = new WrapperPlayServerEntityMetadata(packet.getEntityId(), newEntityMetadata);
            player.sendPacketSilently(newPacket);
            return true;
        }
        return false;
    };*/
}
