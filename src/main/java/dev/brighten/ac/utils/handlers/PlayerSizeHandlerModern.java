package dev.brighten.ac.utils.handlers;

import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerSizeHandlerModern implements PlayerSizeHandler {

    private final WrappedMethod width;
    private final WrappedMethod height;
    private final WrappedMethod gliding;
    private static WrappedClass entityClass = new WrappedClass(Entity.class);

    public PlayerSizeHandlerModern() {
        width = entityClass.getMethod("getWidth");
        height = entityClass.getMethod("getHeight");
        gliding = entityClass.getMethod("isGliding");
    }

    @Override
    @SneakyThrows
    public double height(Player player) {
        return (double) height.invoke(player);
    }

    @Override
    @SneakyThrows
    public double width(Player player) {
        return (double) width.invoke(player);
    }

    @SneakyThrows
    public SimpleCollisionBox bounds(Player player) {
        Location l = player.getLocation();
        double width = (double) this.width.invoke(player)/2;
        double height = this.height.invoke(player);
        return new SimpleCollisionBox().offset(l.getX(), l.getY(), l.getZ()).expand(width,0,width).expandMax(0,height,0);
    }

    @Override
    @SneakyThrows
    public SimpleCollisionBox bounds(Player player, double x, double y, double z) {
        double width = (double) this.width.invoke(player)/2;
        double height = this.height.invoke(player);
        return new SimpleCollisionBox().offset(x,y,z).expand(width,0,width).expandMax(0,height,0);
    }

}
