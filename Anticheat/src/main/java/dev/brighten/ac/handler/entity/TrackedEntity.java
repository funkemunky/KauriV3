package dev.brighten.ac.handler.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TrackedEntity {
    private final int entityId;
    private final EntityType entityType;
    private KLocation location;
    private EntityLocation oldEntityLocation, newEntityLocation;
    private List<FakeMob> fakeMobs = new ArrayList<>();

    public TrackedEntity(int entityId, EntityType entityType, KLocation location) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.location = location;
        this.newEntityLocation = new EntityLocation(this);

        newEntityLocation.x = location.getX();
        newEntityLocation.y = location.getY();
        newEntityLocation.z = location.getZ();
        newEntityLocation.location = location;
        newEntityLocation.yaw = location.getYaw();
        newEntityLocation.pitch = location.getPitch();
    }
    
    public KLocation getEyeLocation() {
        return new KLocation(location.getX(), location.getY() + eyeHeight, location.getZ(),
                location.getYaw(), location.getPitch());
    }
}
