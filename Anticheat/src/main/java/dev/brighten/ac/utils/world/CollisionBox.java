package dev.brighten.ac.utils.world;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

import java.util.List;

public interface CollisionBox {
    boolean isCollided(CollisionBox other);
    boolean isIntersected(CollisionBox other);
    CollisionBox copy();
    CollisionBox offset(double x, double y, double z);
    CollisionBox shrink(double x, double y, double z);
    CollisionBox expand(double x, double y, double z);
    void draw(ParticleType<?> particle, APlayer... players);
    void downCast(List<SimpleCollisionBox> list);
    List<SimpleCollisionBox> downCast();

    boolean isNull();
}