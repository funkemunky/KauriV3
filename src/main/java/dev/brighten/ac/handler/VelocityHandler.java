package dev.brighten.ac.handler;

import dev.brighten.ac.data.APlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityHandler {

    private APlayer player;

    public void onPre( packet) {
        if(packet.getEntityId() != player.getBukkitPlayer().getEntityId()) return;
    }
}
