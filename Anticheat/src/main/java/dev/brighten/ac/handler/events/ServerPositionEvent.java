package dev.brighten.ac.handler.events;

import dev.brighten.ac.utils.KLocation;

public record ServerPositionEvent(int id, double x, double y, double z, float yaw, float pitch) {

    public KLocation getKLocation() {
        return new KLocation(x, y, z, yaw, pitch);
    }
}
