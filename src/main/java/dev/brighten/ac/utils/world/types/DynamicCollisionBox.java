package dev.brighten.ac.utils.world.types;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.world.CollisionBox;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.List;

public class DynamicCollisionBox implements CollisionBox {

    private final CollisionFactory box;
    private APlayer player;
    @Setter
    private WrappedBlock block;
    @Setter
    private ProtocolVersion version;
    private double x,y,z;

    public DynamicCollisionBox(CollisionFactory box, APlayer player, WrappedBlock block, ProtocolVersion version) {
        this.box = box;
        this.player = player;
        this.block = block;
        this.version = version;
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        return box.fetch(version, player, block).offset(x,y,z).isCollided(other);
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        return box.fetch(version, player, block).offset(x, y, z).isIntersected(other);
    }

    @Override
    public DynamicCollisionBox copy() {
        return new DynamicCollisionBox(box, player, block, version).offset(x,y,z);
    }

    @Override
    public DynamicCollisionBox offset(double x, double y, double z) {
        this.x+=x;
        this.y+=y;
        this.z+=z;
        return this;
    }

    @Override
    public DynamicCollisionBox shrink(double x, double y, double z) {
        return this;
    }

    @Override
    public DynamicCollisionBox expand(double x, double y, double z) {
        return this;
    }

    @Override
    public void draw(EnumParticle particle, Player... players) {
        box.fetch(version, player, block).offset(x,y,z).draw(particle,players);
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        box.fetch(version, player, block).offset(x,y,z).downCast(list);
    }

    @Override
    public boolean isNull() {
        return box.fetch(version, player, block).isNull();
    }
}