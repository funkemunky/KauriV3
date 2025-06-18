package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.annotation.Init;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Init
public class JoinListener implements Listener {

    public JoinListener() {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerLoginEvent event) {

        if(event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());

        player.getCheckHandler().callEvent(event);
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerKickEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getPlayer().getUniqueId());
    }
}
