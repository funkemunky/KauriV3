package dev.brighten.ac.utils.math;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.util.Vector;

import java.util.Objects;

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
        return String.format("IntVector[%s, %s, %s]", x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntVector intVector = (IntVector) o;
        return x == intVector.x && y == intVector.y && z == intVector.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
