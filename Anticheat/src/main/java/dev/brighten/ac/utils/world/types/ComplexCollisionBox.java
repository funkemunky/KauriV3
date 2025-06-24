package dev.brighten.ac.utils.world.types;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.world.CollisionBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComplexCollisionBox implements CollisionBox {
    private final List<CollisionBox> boxes = new ArrayList<>();

    public ComplexCollisionBox(CollisionBox... boxes) {
        Collections.addAll(this.boxes, boxes);
    }

    public boolean add(CollisionBox collisionBox) {
        return boxes.add(collisionBox);
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        return boxes.stream().anyMatch(box -> box.isCollided(other));
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        return boxes.stream().anyMatch(box -> box.isIntersected(other));
    }

    @Override
    public ComplexCollisionBox copy() {
        ComplexCollisionBox cc = new ComplexCollisionBox();
        for (CollisionBox b : boxes)
            cc.boxes.add(b.copy());
        return cc;
    }

    @Override
    public ComplexCollisionBox offset(double x, double y, double z) {
        for (CollisionBox b : boxes)
            b.offset(x,y,z);
        return this;
    }

    @Override
    public ComplexCollisionBox shrink(double x, double y, double z) {
        for (CollisionBox b : boxes)
            b.shrink(x,y,z);
        return this;
    }

    @Override
    public ComplexCollisionBox expand(double x, double y, double z) {
        for (CollisionBox b : boxes)
            b.expand(x,y,z);
        return this;
    }

    @Override
    public void draw(ParticleType<?> particle, APlayer... players) {
        for (CollisionBox b : boxes)
            b.draw(particle,players);
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        for (CollisionBox box : boxes)
            box.downCast(list);
    }

    @Override
    public List<SimpleCollisionBox> downCast() {
        List<SimpleCollisionBox> list = new ArrayList<>();

        for (CollisionBox box : boxes) {
            box.downCast(list);
        }
        return list;
    }

    @Override
    public boolean isNull() {
        for(CollisionBox box : boxes)
            if (!box.isNull())
                return false;
        return true;
    }

}