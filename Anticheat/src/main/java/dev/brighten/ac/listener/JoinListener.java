package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.utils.annotation.Init;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(EventPriority.NORMAL, event -> {
            if(event.isCancelled() || !Anticheat.INSTANCE.isEnabled()) return;
            Optional<APlayer> aplayer = Anticheat.INSTANCE.getPlayerRegistry()
                    .getPlayer(event.getPlayer().getUniqueId());

            aplayer.ifPresent(player -> {
                if(!player.isInitialized()) {
                    return;
                }
                if(Anticheat.INSTANCE.getPacketHandler()
                        .process(player, event.getType(), event.getPacket())) {
                    event.setCancelled(true);
                }
            });
        });

        Anticheat.INSTANCE.getPacketProcessor().process(EventPriority.HIGHEST, event -> {
            if(!Anticheat.INSTANCE.isEnabled())
                return;
            Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId());

            if(op.isEmpty()) {
                return;
            }

            APlayer player = op.get();

            if(!player.isInitialized()) return;

            if(event.getType().equals(PacketType.CLIENT_TRANSACTION)) {
                if(!player.getPacketQueue().isEmpty()) {
                    player.setSendingPackets(true);
                    Object packetToSend;
                    synchronized (player.getPacketQueue()) {
                        while((packetToSend = player.getPacketQueue().pollFirst()) != null) {
                            HandlerAbstract.getHandler().sendPacketSilently(player, packetToSend);
                        }
                    }
                    player.setSendingPackets(false);
                }
            } else {
                switch (event.getType()) {
                    case ENTITY, ENTITY_DESTROY, ENTITY_HEAD_ROTATION, ENTITY_MOVE, ENTITY_MOVELOOK, ENTITY_LOOK -> {
                        if (player.getLagInfo().getLastClientTransaction().isPassed(200L)
                                && player.getCreation().isPassed(6000L)) {
                            synchronized (player.getPacketQueue()) {
                                player.getPacketQueue().add(event.getPacket());
                            }
                            event.setCancelled(true);
                        }
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerLoginEvent event) {
        for(int i = 0; i < 10 ; i++) {
            Anticheat.INSTANCE.getLogger().info("Player joined. " + event.getPlayer().getName());
        }

        if(event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());

        if(Anticheat.INSTANCE.getPlayerRegistry().aplayerMap.containsKey(event.getPlayer().getUniqueId().hashCode())) {
            if(Anticheat.INSTANCE.getPlayerRegistry().aplayerMap
                    .containsKey(event.getPlayer().getUniqueId().hashCode())
                    && event.getPlayer() != null) {
                HandlerAbstract.getHandler().add(event.getPlayer());
            }
        }

        player.getCheckHandler().callEvent(event);
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
