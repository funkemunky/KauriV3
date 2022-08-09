package dev.brighten.ac.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.RunUtils;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@Init
public class JoinListener implements Listener {

    public JoinListener() {
        Anticheat.INSTANCE.getPacketProcessor().processAsync(Anticheat.INSTANCE, EventPriority.NORMAL, event -> {
            Optional<APlayer> aplayer = Anticheat.INSTANCE.getPlayerRegistry()
                    .getPlayer(event.getPlayer().getUniqueId());

            aplayer.ifPresent(player -> {
                if(event.getPacket() instanceof WPacket) {
                    Anticheat.INSTANCE.getPacketHandler().process(player, (WPacket) event.getPacket());
                } else {
                    Anticheat.INSTANCE.getPacketHandler().process(player, (Packet) event.getPacket());
                }
            });
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());
        RunUtils.taskLater(() -> HandlerAbstract.getHandler().add(event.getPlayer()), 4L);

        player.callEvent(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getPlayer().getUniqueId());
    }
}
