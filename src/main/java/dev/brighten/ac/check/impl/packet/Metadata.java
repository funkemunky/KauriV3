package dev.brighten.ac.check.impl.packet;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityMetadata;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutSpawnEntityLiving;

@CheckData(name = "Metadata", checkId = "metadata", type = CheckType.BADPACKETS)
public class Metadata extends Check {

    public Metadata(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayOutEntityMetadata> packet = packet -> {
        debug("entityId: " + packet.getEntityId());
        packet.getWatchedObjects().forEach(watchedObject -> {
            debug("watchedObject: " + watchedObject.getObjectType() + "," + watchedObject.getDataValueId() + ", " + watchedObject.getWatchedObject());
        });
    };

    WAction<WPacketPlayOutSpawnEntityLiving> spawn = packet -> {
        debug("(spawned) entityId: " + packet.getEntityId());
        packet.getWatchedObjects().forEach(watchedObject -> {
            if(watchedObject.getDataValueId() == 0) {
                byte bitInfo = (byte) watchedObject.getWatchedObject();
                boolean sneaking = (bitInfo & 1 << 1) != 0;
                boolean sprinting = (bitInfo & 1 << 3) != 0;
                boolean invisible = (bitInfo & 1 << 5) != 0;

                debug("sneaking:%s sprinting:%s invisible:%s", sneaking, sprinting, invisible);
            }
            debug("watchedObject: " + watchedObject.getObjectType() + "," + watchedObject.getDataValueId() + ", " + watchedObject.getWatchedObject());
        });
    };
}
