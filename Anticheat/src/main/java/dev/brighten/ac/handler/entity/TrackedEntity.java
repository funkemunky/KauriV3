package dev.brighten.ac.handler.entity;

import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import dev.brighten.ac.data.obj.Pose;
import dev.brighten.ac.handler.ValuedAttribute;
import dev.brighten.ac.utils.EntityLocation;
import dev.brighten.ac.utils.KLocation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class TrackedEntity implements Cloneable {
    private final int entityId;
    private final EntityType entityType;
    private KLocation location;
    private EntityLocation oldEntityLocation, newEntityLocation;
    private List<FakeMob> fakeMobs = new ArrayList<>();
    private Set<ValuedAttribute> attributes = new HashSet<>();
    private Pose pose = Pose.STANDING;

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
        return new KLocation(location.getX(), location.getY() + pose.eyeHeight, location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public ValuedAttribute getAttribute(Attribute type) {
        for(ValuedAttribute attribute : attributes) {
            if(attribute.getAttribute().equals(type)) {
                return attribute;
            }
        }
        return null;
    }

    public void updateAttribute(WrapperPlayServerUpdateAttributes.Property property) {
        ValuedAttribute attribute = getAttribute(property.getAttribute());

        if(attribute == null) {
            attribute = new ValuedAttribute(property.getAttribute());
        }

        attribute.updateAttribute(property);

        attributes.add(attribute);
    }

    public TrackedEntity clone() {
        try {
            TrackedEntity c = (TrackedEntity) super.clone();
            // Perform deep cloning for mutable fields
            c.location = location != null ? location.clone() : null;
            c.oldEntityLocation = oldEntityLocation != null ? oldEntityLocation.clone() : null;
            c.newEntityLocation = newEntityLocation != null ? newEntityLocation.clone() : null;
            c.fakeMobs = new ArrayList<>(fakeMobs);
            c.attributes = new HashSet<>(attributes);
            c.pose = pose;
            
            return c;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("This should never happen", e);
        }
    }
}
