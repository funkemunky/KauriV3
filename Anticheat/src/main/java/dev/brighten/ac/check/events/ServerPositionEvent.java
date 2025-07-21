package dev.brighten.ac.check.events;

import dev.brighten.ac.utils.KLocation;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ServerPositionEvent {

    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public ServerPositionEvent(int id, double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public KLocation getKLocation() {
        return new KLocation(x, y, z, yaw, pitch);
    }
}
