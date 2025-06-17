package dev.brighten.ac.handler;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityVelocity;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutExplosion;
import dev.brighten.ac.utils.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class VelocityHandler {

    private final APlayer PLAYER;

    private final List<Tuple<Vector, Boolean>> VELOCITY_MAP = new LinkedList<>();
    private final Set<Consumer<Vector>> VELOCITY_TASKS = new HashSet<>();

    

    /*
     * I want to be able to verify velocity when the pre packet comes back an the post packet comes back
     * So essentially I want to only take out the velocity from possibilities after the post flying comes back.
     */

    public void onPre(WPacketPlayOutEntityVelocity packet) {
        if(packet.getEntityId() != PLAYER.getBukkitPlayer().getEntityId()) return;

        VELOCITY_MAP.add(new Tuple<>(new Vector(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ()), false));
    }

    public void onPre(WPacketPlayOutExplosion packet) {
        VELOCITY_MAP.add(new Tuple<>(packet.getEntityPush().toBukkitVector(), false));
    }

    public void onPost(WPacketPlayOutEntityVelocity packet) {
        if(packet.getEntityId() != PLAYER.getBukkitPlayer().getEntityId()) return;

        for (Tuple<Vector, Boolean> set : VELOCITY_MAP) {
            if(set.two) continue;
            if(set.one.equals(new Vector(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ()))) {
                set.two = true;
            }
        }
    }

    public void onPost(WPacketPlayOutExplosion packet) {
        for (Tuple<Vector, Boolean> set : VELOCITY_MAP) {
            if(set.two) continue;

            if(set.one.equals(packet.getEntityPush().toBukkitVector())) {
                set.two = true;
            }
        }
    }

    public List<Vector> getPossibleVectors() {
        return VELOCITY_MAP.stream()
                .map(set -> set.one)
                .toList();
    }

    public void onAccurateVelocity(Consumer<Vector> task) {
        VELOCITY_TASKS.add(task);
    }

    public void onFlyingPost(WrapperPlayClientPlayerFlying packet) {
        val iterator = VELOCITY_MAP.iterator();
        while(iterator.hasNext()) {
            val value = iterator.next();

            // Velocity definitely occurred, run task.
            if(Math.abs(value.one.getY() - packet.getY()) < 1E-6) {
                VELOCITY_TASKS.forEach(vel -> vel.accept(value.one));
            }

            if(value.two)
                iterator.remove();
        }
    }


}
