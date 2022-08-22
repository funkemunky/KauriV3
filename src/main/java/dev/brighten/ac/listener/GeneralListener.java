package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Init;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@Init
public class GeneralListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player) {
            APlayer player = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getDamager().getUniqueId()).
                    orElse(null);

            if(player == null) return;

            if(player.hitsToCancel > 0) {
                player.hitsToCancel--;
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getBlockUpdateHandler().onWorldChange());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.callEvent(event));
    }
}
