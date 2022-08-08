package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.RunUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(Anticheat.INSTANCE, EventPriority.NORMAL, event -> {
            if(event.getType().equals(PacketType.FLYING)) {
                WPacketPlayInFlying flying = Anticheat.INSTANCE.getPacketProcessor().getPacketConverter()
                        .processFlying(event.getPacket());

                event.getPlayer().sendMessage(flying.isLooked() + ";"  + flying.isMoved() + ";"
                        + flying.isOnGround() + ":" + flying.getX() + ":" + flying.getYaw());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.broadcastMessage("Joined");
        RunUtils.taskLater(() -> HandlerAbstract.getHandler().add(event.getPlayer()), 4L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        HandlerAbstract.getHandler().remove(event.getPlayer());
    }
}
