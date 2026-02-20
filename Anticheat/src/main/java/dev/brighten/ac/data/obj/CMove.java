package dev.brighten.ac.data.obj;

import dev.brighten.ac.handler.block.World;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CMove {
    private KLocation loc = new KLocation(0,0,0,0,0);
    private World world;
    private SimpleCollisionBox box = new SimpleCollisionBox(new KLocation(0,0,0,0,0), 0, 0);
    private boolean onGround;
    public void setLoc(CMove move) {
        this.loc = move.getLoc().clone();
        this.world = move.getWorld();
        this.box = move.getBox().copy();
        this.onGround = move.isOnGround();
    }

    public double getX() {
        return loc.getX();
    }

    public double getY() {
        return loc.getY();
    }

    public double getZ() {
        return loc.getZ();
    }

    public float getYaw() {
        return loc.getYaw();
    }

    public float getPitch() {
        return loc.getPitch();
    }
}
