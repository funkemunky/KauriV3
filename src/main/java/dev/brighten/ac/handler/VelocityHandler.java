package dev.brighten.ac.handler;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityVelocity;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.*;

@RequiredArgsConstructor
public class VelocityHandler {

    private final APlayer player;

    private Map<Vector, Boolean> velocities = new HashMap<>();
    private Set<Consumer<Vector>> accurateVelocityTasks = new HashSet<>();

    

    /*
     * I want to be able to verify velocity when the pre packet comes back an the post packet comes back
     * So essentially I want to only take out the velocity from possibilities after the post flying comes back.
     */

    public void onPre(WPacketPlayOutEntityVelocity packet) {
        if(packet.getEntityId() != player.getBukkitPlayer().getEntityId()) return;

        velocities.put(new Vector(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ()), false);
    }

    public void onPost(WPacketPlayOutEntityVelocity packet) {
        if(packet.getEntityId() != player.getBukkitPlayer().getEntityId()) return;

        velocities.computeIfPresent(new Vector(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ()),
                (velocity, queuedToRemove) -> true);
    }

    public Set<Vector> getPossibleVectors() {
        return velocities.keySet();
    }

    public void onAccurateVelocity(Consumer<Vector> task) {
        accurateVelocityTasks.add(task);
    }

    public void onFlyingPost(WPacketPlayInFlying packet) {
        val iterator = velocities.entrySet().iterator();
        while(iterator.hasNext()) {
            val value = iterator.next();

            // Velocity definitely occurred, run task.
            if(Math.abs(value.getKey().getY() - packet.getY()) < 1E-6) {
                accurateVelocityTasks.forEach(vel -> vel.accept(value.getKey()));
            }

            if(value.getValue())
                iterator.remove();
        }
    }


}
