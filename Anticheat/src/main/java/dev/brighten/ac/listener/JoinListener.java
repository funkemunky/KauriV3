package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.utils.annotation.Init;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(EventPriority.NORMAL, event -> {
            if(event.isCancelled()) return;
            Optional<APlayer> aplayer = Anticheat.INSTANCE.getPlayerRegistry()
                    .getPlayer(event.getPlayer().getUniqueId());

            aplayer.ifPresent(player -> {
                if(Anticheat.INSTANCE.getPacketHandler()
                        .process(player, event.getType(), event.getPacket())) {
                    event.setCancelled(true);
                }
            });
        });

        Anticheat.INSTANCE.getPacketProcessor().process(EventPriority.HIGHEST, event -> {
            Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(event.getPlayer().getUniqueId());

            if(!op.isPresent()) {
                return;
            }

            APlayer player = op.get();

            if(player.isSendingPackets()) return;

            if(event.getType().equals(PacketType.CLIENT_TRANSACTION)) {
                if(player.getPacketQueue().size() > 0) {
                    player.setSendingPackets(true);
                    Object packetToSend = null;
                    synchronized (player.getPacketQueue()) {
                        while((packetToSend = player.getPacketQueue().pollFirst()) != null) {
                            HandlerAbstract.getHandler().sendPacketSilently(player, packetToSend);
                        }
                    }
                    player.setSendingPackets(false);
                }
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
                        if(player.getLagInfo().getLastClientTransaction().isPassed(200L) && player.getCreation().isPassed(6000L)) {
                            synchronized (player.getPacketQueue()) {
                                player.getPacketQueue().add(event.getPacket());
                            }
                            event.setCancelled(true);
                        }
                        break;
                    }
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());

        if(Anticheat.INSTANCE.getPlayerRegistry().aplayerMap.containsKey(event.getPlayer().getUniqueId().hashCode())) {
            if(Anticheat.INSTANCE.getPlayerRegistry().aplayerMap
                    .containsKey(event.getPlayer().getUniqueId().hashCode())
                    && event.getPlayer() != null && event.getPlayer().isOnline()) {
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
