package dev.brighten.ac.utils.world;

import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.entity.Player;

import java.util.List;

public interface CollisionBox {
    boolean isCollided(CollisionBox other);
    boolean isIntersected(CollisionBox other);
    CollisionBox copy();
    CollisionBox offset(double x, double y, double z);
    CollisionBox shrink(double x, double y, double z);
    CollisionBox expand(double x, double y, double z);
    void draw(EnumParticle particle, Player... players);
    void downCast(List<SimpleCollisionBox> list);
    boolean isNull();
}