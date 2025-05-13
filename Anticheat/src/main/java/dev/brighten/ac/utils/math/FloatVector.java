package dev.brighten.ac.utils.math;

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
public class FloatVector implements Cloneable {
    private float x, y, z;

    public FloatVector clone() {
        try {
            return (FloatVector) super.clone();
        } catch(CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public Vector toBukkitVector() {
        return new Vector(x, y, z);
    }

    @Override
    public String toString() {
        return "FloatVector[" + x + ", " +  y + ", " + z + "]";
    }

    public FloatVector add(float x, float y, float z) {
        this.x+= x;
        this.y+= y;
        this.z+= z;
        return this;
    }

    public FloatVector add(FloatVector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FloatVector that)) return false;
        return Float.compare(x, that.x) == 0 && Float.compare(y, that.y) == 0 && Float.compare(z, that.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
