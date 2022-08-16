package dev.brighten.ac.utils.world.types;

import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.world.CollisionBox;
import org.bukkit.entity.Player;

import java.util.List;

public class NoCollisionBox implements CollisionBox {

    public static final NoCollisionBox INSTANCE = new NoCollisionBox();

    private NoCollisionBox() { }

    @Override
    public boolean isCollided(CollisionBox other) {
        return false;
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        return false;
    }

    @Override
    public NoCollisionBox offset(double x, double y, double z) {
        return this;
    }

    @Override
    public NoCollisionBox shrink(double x, double y, double z) {
        return this;
    }

    @Override
    public NoCollisionBox expand(double x, double y, double z) {
        return this;
    }

    @Override
    public void draw(EnumParticle particle, Player... players) {

    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) { /**/ }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public NoCollisionBox copy() {
        return this;
    }
}