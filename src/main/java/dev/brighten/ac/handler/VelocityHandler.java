package dev.brighten.ac.handler;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityVelocity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityHandler {

    private APlayer player;

    public void onPre(WPacketPlayOutEntityVelocity packet) {
        if(packet.getEntityId() != player.getBukkitPlayer().getEntityId()) return;
    }
}
