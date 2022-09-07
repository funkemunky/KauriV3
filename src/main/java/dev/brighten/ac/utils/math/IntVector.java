package dev.brighten.ac.utils.math;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.util.Vector;

@AllArgsConstructor
@NoArgsConstructor
public class IntVector {
    @Getter
    @Setter
    private int x, y, z;

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
