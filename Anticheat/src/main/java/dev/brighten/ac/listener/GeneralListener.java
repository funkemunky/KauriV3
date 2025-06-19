package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.world.types.RayCollision;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;

@Init
public class GeneralListener implements Listener {

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getInfo().breakingBlock = event.getAction()
                        .equals(Action.LEFT_CLICK_BLOCK));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getCheckHandler().callEvent(event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getWhoClicked().getUniqueId())
                .ifPresent(player -> player.getCheckHandler().callEvent(event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getFrom()
                .getWorld()
                .equals(event
                        .getTo()
                        .getWorld())) return;

        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> {
                    player.getBlockUpdateHandler().onWorldChange();

                    // Updating bot loc when changing worlds
                    Location origin = event.getTo().clone().add(0, 1.7, 0);

                    RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection().multiply(-1));

                    Vector loc1 = coll.collisionPoint(1.2);

                    Anticheat.INSTANCE.getRunUtils().taskLater(() -> {
                        player.getMob().despawn();
                        player.getMob().spawn(true, new KLocation(loc1), new ArrayList<>(), player);
                    }, 5);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getCheckHandler().callEvent(event));
    }
}
