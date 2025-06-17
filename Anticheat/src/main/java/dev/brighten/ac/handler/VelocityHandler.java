package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class VelocityHandler {

    private final APlayer PLAYER;

    private final List<Tuple<Vector3d, Boolean>> VELOCITY_MAP = new LinkedList<>();
    private final Set<Consumer<Vector3d>> VELOCITY_TASKS = new HashSet<>();

    

    /*
     * I want to be able to verify velocity when the pre packet comes back an the post packet comes back
     * So essentially I want to only take out the velocity from possibilities after the post flying comes back.
     */

    public void onPre(WrapperPlayServerEntityVelocity packet) {
        if(packet.getEntityId() != PLAYER.getBukkitPlayer().getEntityId()) return;

        VELOCITY_MAP.add(new Tuple<>(packet.getVelocity(), false));
    }

    public void onPre(WrapperPlayServerExplosion packet) {
        Vector3d kb = packet.getKnockback();

        if(kb == null) return;

        VELOCITY_MAP.add(new Tuple<>(kb, false));
    }

    public void onPost(WrapperPlayServerEntityVelocity packet) {
        if(packet.getEntityId() != PLAYER.getBukkitPlayer().getEntityId()) return;

        for (Tuple<Vector3d, Boolean> set : VELOCITY_MAP) {
            if(set.two) continue;
            if(set.one.equals(packet.getVelocity())) {
                set.two = true;
            }
        }
    }

    public void onPost(WrapperPlayServerExplosion packet) {
        for (Tuple<Vector3d, Boolean> set : VELOCITY_MAP) {
            if(set.two) continue;

            if(set.one.equals(packet.getKnockback())) {
                set.two = true;
            }
        }
    }

    public List<Vector3d> getPossibleVectors() {
        return VELOCITY_MAP.stream()
                .map(set -> set.one)
                .toList();
    }

    public void onAccurateVelocity(Consumer<Vector3d> task) {
        VELOCITY_TASKS.add(task);
    }

    public void onFlyingPost(WrapperPlayClientPlayerFlying packet) {
        val iterator = VELOCITY_MAP.iterator();
        while(iterator.hasNext()) {
            val value = iterator.next();

            // Velocity definitely occurred, run task.
            if(Math.abs(value.one.getY() - packet.getLocation().getY()) < 1E-6) {
                VELOCITY_TASKS.forEach(vel -> vel.accept(value.one));
            }

            if(value.two)
                iterator.remove();
        }
    }


}
