package dev.brighten.ac.utils.math;

import dev.brighten.ac.utils.MathHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

@AllArgsConstructor
@NoArgsConstructor
public class IntVector {
    @Getter
    @Setter
    private int x, y, z;

    public IntVector(Location location) {
        this.x = MathHelper.floor_double(location.getX());
        this.y = MathHelper.floor_double(location.getY());
        this.z = MathHelper.floor_double(location.getZ());
    }

    public IntVector clone() {
        return new IntVector(x, y, z);
    }

    public Vector toBukkitVector() {
        return new Vector(x, y, z);
    }

    @Override
    public String toString() {
        return "IntVector[" + x + ", " +  y + ", " + z + "]";
    }

    public IntVector add(int x, int y, int z) {
        this.x+= x;
        this.y+= y;
        this.z+= z;
        return this;
    }

    public Integer[] toIntArray() {
        return new Integer[] {x, y, z};
    }

    public IntVector add(IntVector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof IntVector)) return false;

        IntVector intVector = (IntVector) o;
        return x == intVector.x && y == intVector.y && z == intVector.z;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
        hash = 79 * hash + (int)(Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
        return hash;
    }
}
