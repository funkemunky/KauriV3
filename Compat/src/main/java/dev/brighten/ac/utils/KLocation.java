package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.util.MathUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;

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

    public KLocation(Location location) {
        this(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public KLocation(Vector3d vector) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
        this.timeStamp = System.currentTimeMillis();
    }

    public KLocation(KLocation location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.timeStamp = System.currentTimeMillis();
    }

    public Vector3d toVector() {
        return new Vector3d(x, y, z);
    }

    public Vector3i toVector3i() {
        return new Vector3i((int) x, (int) y, (int) z);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public KLocation clone() {
        try {
            return (KLocation) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KLocation(x, y, z, yaw, pitch, timeStamp);
        }
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

    public Vector3d getDirection() {
        double rotX = this.getYaw();
        double rotY = this.getPitch();
        double x, y, z;
        y = -Math.sin(Math.toRadians(rotY));
        double xz = Math.cos(Math.toRadians(rotY));
        x = -xz * Math.sin(Math.toRadians(rotX));
        z = xz * Math.cos(Math.toRadians(rotX));
        return new Vector3d(x, y, z);
    }

    public int getBlockX() {
        return MathUtil.floor(x);
    }

    public int getBlockY() {
        return MathUtil.floor(y);
    }

    public int getBlockZ() {
        return MathUtil.floor(z);
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
