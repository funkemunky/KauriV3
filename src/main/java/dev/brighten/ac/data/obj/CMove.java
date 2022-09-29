package dev.brighten.ac.data.obj;

import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;

@Getter
@Setter
public class CMove {
    private KLocation loc = new KLocation(0,0,0,0,0);
    private World world;
    private SimpleCollisionBox box = new SimpleCollisionBox(new KLocation(0,0,0,0,0), 0.6f, 1.8f);
    private boolean onGround;
    public void setLoc(CMove move) {
        this.loc = move.getLoc().clone();
        this.world = move.getWorld();
        this.box = move.getBox();
        this.onGround = move.isOnGround();
    }

    public double getX() {
        return loc.x;
    }

    public double getY() {
        return loc.y;
    }

    public double getZ() {
        return loc.z;
    }

    public float getYaw() {
        return loc.yaw;
    }

    public float getPitch() {
        return loc.pitch;
    }
}
