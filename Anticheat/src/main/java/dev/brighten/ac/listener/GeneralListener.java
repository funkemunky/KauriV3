package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.utils.RunUtils;
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
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.Collections;

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
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId()).ifPresent(player -> {
            player.getInfo().breakingBlock = event.getAction().equals(Action.LEFT_CLICK_BLOCK);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getCheckHandler().callEvent(event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> {
                    player.getBlockUpdateHandler().onWorldChange();

                    // Updating bot loc when changing worlds
                    Location origin = event.getTo().clone().add(0, 1.7, 0);

                    RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection().multiply(-1));

                    Location loc1 = coll.collisionPoint(1.2).toLocation(event.getTo().getWorld());

                    RunUtils.taskLater(() -> {
                        player.getMob().despawn();
                        player.getMob().spawn(true, loc1,
                                new ArrayList<>(Collections.singletonList(
                                        new WrappedWatchableObject(0, 16, (byte) 1))), player);
                    }, 5);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getCheckHandler().callEvent(event));
    }
}
