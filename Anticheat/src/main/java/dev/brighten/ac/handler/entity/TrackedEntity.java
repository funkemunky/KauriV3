package dev.brighten.ac.handler.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.brighten.ac.utils.KLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class TrackedEntity {
    private int entityId;
    private EntityType entityType;
    private KLocation location;
}
