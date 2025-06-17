package dev.brighten.ac.utils.world.types;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.world.CollisionBox;

import java.util.Collections;
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
    public void draw(ParticleType<?> particle, APlayer... players) {

    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) { /**/ }

    @Override
    public List<SimpleCollisionBox> downCast() {
        return Collections.emptyList();
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public NoCollisionBox copy() {
        return this;
    }
}