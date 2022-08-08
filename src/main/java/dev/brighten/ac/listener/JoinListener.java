package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.RunUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(Anticheat.INSTANCE, EventPriority.NORMAL, event -> {
            if(event.getType().equals(PacketType.BLOCK_DIG)) {

                event.getPlayer().sendMessage("BlockDig:");
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RunUtils.taskLater(() -> HandlerAbstract.getHandler().add(event.getPlayer()), 4L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        HandlerAbstract.getHandler().remove(event.getPlayer());
    }
}
