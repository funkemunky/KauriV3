package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.RunUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(Anticheat.INSTANCE, EventPriority.NORMAL, event -> {
            if(event.isCancelled()) return;
            Optional<APlayer> aplayer = Anticheat.INSTANCE.getPlayerRegistry()
                    .getPlayer(event.getPlayer().getUniqueId());

            aplayer.ifPresent(player -> Anticheat.INSTANCE.getPacketHandler()
                    .process(player, event.getType(), event.getPacket()));
        });

        Anticheat.INSTANCE.getPacketProcessor().process(Anticheat.INSTANCE, EventPriority.HIGHEST, event -> {
            Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId());

            if(!op.isPresent()) {
                return;
            }

            APlayer player = op.get();

            if(player.isSendingPackets()) return;

            if(event.getType().equals(PacketType.CLIENT_TRANSACTION)) {
                player.setSendingPackets(true);
                Object packetToSend = null;

                synchronized (player.getPacketQueue()) {
                    while((packetToSend = player.getPacketQueue().pollFirst()) != null) {
                        HandlerAbstract.getHandler().sendPacket(player, packetToSend);
                    }
                }
                player.setSendingPackets(false);
            } else {
                switch (event.getType()) {
                    case ENTITY:
                    case ENTITY_DESTROY:
                    case ENTITY_HEAD_ROTATION:
                    case ENTITY_MOVE:
                    case ENTITY_MOVELOOK:
                    case ENTITY_LOOK:
                    case BLOCK_CHANGE:
                    case MULTI_BLOCK_CHANGE:
                    case MAP_CHUNK: {
                        synchronized (player.getPacketQueue()) {
                            player.getPacketQueue().add(event.getPacket());
                        }
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        });
    }

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
    public void onJoin(PlayerJoinEvent event) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());

        RunUtils.taskLater(() -> HandlerAbstract.getHandler().add(event.getPlayer()), 6);

        player.callEvent(event);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> player.getBlockUpdateHandler().onWorldChange());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HandlerAbstract.getHandler().remove(event.getPlayer());
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerKickEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getPlayer().getUniqueId());
    }
}
