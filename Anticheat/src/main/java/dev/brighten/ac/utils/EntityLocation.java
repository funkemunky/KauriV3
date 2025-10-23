package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.util.Vector3d;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import lombok.RequiredArgsConstructor;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.Deque;

@RequiredArgsConstructor
//Duplicate class for obfuscation purposes
public class EntityLocation {
    public final TrackedEntity entity;
    public double newX, newY, newZ, x, y, z;
    public float newYaw, newPitch, yaw, pitch;
    public int increment = 0;
    public boolean sentTeleport = false;
    public KLocation location;
    public Deque<KLocation> interpolatedLocations = new EvictingList<>(2);

    public void interpolateLocation() {
        if(increment > 0) {
            double d0 = x + (newX - x) / increment;
            double d1 = y + (newY - y) / increment;
            double d2 = z + (newZ - z) / increment;
            double d3 = MathHelper.wrapAngleTo180_float(newYaw - yaw);

            yaw = (float) ((double) yaw + d3 / (double) increment);
            pitch = (float) ((double) pitch + (newPitch - (double) pitch) / (double) increment);

            increment--;

            this.x = d0;
            this.y = d1;
            this.z = d2;
            entity.setLocation(new KLocation(x, y, z, yaw, pitch));
            interpolatedLocations.add(new KLocation(x, y, z, yaw, pitch));
        }
    }

    public EntityLocation clone() {
        final EntityLocation loc = new EntityLocation(entity);

        loc.x = x;
        loc.y = y;
        loc.z = z;
        loc.yaw = yaw;
        loc.pitch = pitch;
        loc.increment = increment;
        loc.newX = newX;
        loc.newY = newY;
        loc.newZ = newZ;
        loc.newYaw = newYaw;
        loc.newPitch = newPitch;
        loc.sentTeleport = sentTeleport;
        loc.interpolatedLocations.addAll(interpolatedLocations);

        return loc;
    }

    public Vector3d getCurrentIteration() {
        return new Vector3d(x, y, z);
    }
}