package dev.brighten.ac.utils;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Objects;

@Data
public class KLocation implements Cloneable {
    private double x, y, z;
    private float yaw, pitch;
    private long timeStamp;

    public KLocation(double x, double y, double z, float yaw, float pitch, long timeStamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timeStamp = timeStamp;
    }

    public KLocation(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timeStamp = System.currentTimeMillis();
    }

    public KLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.timeStamp = System.currentTimeMillis();
    }

    public KLocation(Vector vector) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
        this.timeStamp = System.currentTimeMillis();
    }

    public KLocation(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.timeStamp = System.currentTimeMillis();
    }

    public Vector toVector() {
        return new Vector(x, y, z);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public KLocation clone() {
        return new KLocation(x, y, z, yaw, pitch, timeStamp);
    }

    public double distanceSquared(KLocation other) {
        double dx = (x - other.x), dy = (y - other.y), dz = (z - other.z);
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(KLocation other) {
        return Math.sqrt(distanceSquared(other));
    }

    public KLocation add(double x, double y, double z) {
        this.x+= x;
        this.y+= y;
        this.z+= z;
        return this;
    }

    public KLocation subtract(double x, double y, double z) {
        this.x-= x;
        this.y-= y;
        this.z-= z;
        return this;
    }

    public KLocation setLocation(KLocation loc) {
        this.x = loc.x;
        this.y = loc.y;
        this.z = loc.z;
        this.yaw = loc.yaw;
        this.pitch = loc.pitch;
        this.timeStamp = loc.timeStamp;

        return this;
    }

    public Vector getDirection() {
        return MathUtils.getDirection(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KLocation kLocation = (KLocation) o;
        return Double.compare(kLocation.x, x) == 0 && Double.compare(kLocation.y, y) == 0 && Double.compare(kLocation.z, z) == 0 && Float.compare(kLocation.yaw, yaw) == 0 && Float.compare(kLocation.pitch, pitch) == 0 && timeStamp == kLocation.timeStamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, yaw, pitch, timeStamp);
    }

    @Override
    public String toString() {
        return "KLocation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
