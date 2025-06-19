package dev.brighten.ac.utils.math;

import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.utils.KLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Objects;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IntVector implements Cloneable {
    private int x, y, z;

    public IntVector(Location location) {
        this.x = MathHelper.floor_double(location.getX());
        this.y = MathHelper.floor_double(location.getY());
        this.z = MathHelper.floor_double(location.getZ());
    }

    public IntVector(KLocation location) {
        this.x = MathHelper.floor_double(location.getX());
        this.y = MathHelper.floor_double(location.getY());
        this.z = MathHelper.floor_double(location.getZ());
    }

    public IntVector(Vector3i vector) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
    }

    public IntVector clone() {
        final IntVector clone;

        try {
            clone = (IntVector) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Failed to clone IntVector", ex);
        }

        return clone;
    }

    public Vector toBukkitVector() {
        return new Vector(x, y, z);
    }

    @Override
    public String toString() {
        return "[" + x + ", " +  y + ", " + z + "]";
    }

    public IntVector add(int x, int y, int z) {
        this.x+= x;
        this.y+= y;
        this.z+= z;
        return this;
    }

    public IntVector add(IntVector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntVector intVector)) return false;
        return x == intVector.x && y == intVector.y && z == intVector.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
