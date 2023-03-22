package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import lombok.RequiredArgsConstructor;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@RequiredArgsConstructor
//Duplicate class for obfuscation purposes
public class EntityLocation {
    public final Entity entity;
    public double newX, newY, newZ, x, y, z;
    public float newYaw, newPitch, yaw, pitch;
    public int increment = 0;
    public boolean sentTeleport = false;
    public KLocation location;
    public Deque<KLocation> interpolatedLocations = new EvictingList<>(2);

    public void interpolateLocations() {
        increment = 3;
        while(increment > 0) {
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
            interpolatedLocations.add(new KLocation(x, y, z, yaw, pitch, Anticheat.INSTANCE.getKeepaliveProcessor().tick));
        }
    }

    public void interpolateRestOfLocations() {
        while(increment > 0) {
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
            interpolatedLocations.add(new KLocation(x, y, z, yaw, pitch, Anticheat.INSTANCE.getKeepaliveProcessor().tick));
        }
    }

    public List<KLocation> getInterpolatedLocations() {
        int increment = 3;
        double x = this.x, y = this.y, z = this.z, newX = this.newX, newY = this.newY, newZ = this.newZ;
        float yaw = this.yaw, pitch = this.pitch, newYaw = this.newYaw, newPitch = this.newPitch;
        List<KLocation> locations = new ArrayList<>();
        while(increment > 0) {
            double d0 = x + (newX - x) / increment;
            double d1 = y + (newY - y) / increment;
            double d2 = z + (newZ - z) / increment;
            double d3 = MathHelper.wrapAngleTo180_float(newYaw - yaw);

            yaw = (float) ((double) yaw + d3 / (double) increment);
            pitch = (float) ((double) pitch + (newPitch - (double) pitch) / (double) increment);

            increment--;

            x = d0;
            y = d1;
            z = d2;
            locations.add(new KLocation(x, y, z, yaw, pitch, Anticheat.INSTANCE.getKeepaliveProcessor().tick));
        }

        return locations;
    }

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

    public Vector getCurrentIteration() {
        return new Vector(x, y, z);
    }
}