package dev.brighten.ac.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;

import java.util.Optional;
import java.util.logging.Level;

public class PacketListener implements com.github.retrooper.packetevents.event.PacketListener {

    @Override
    public void onUserLogin(UserLoginEvent event) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().generate(event.getPlayer());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        Anticheat.INSTANCE.getPlayerRegistry().unregister(event.getUser().getUUID());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle packet receive events here
        User user = event.getUser();

        if(user == null || user.getUUID() == null) {
            return;
        }

        try {
            Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(user.getUUID());


            if(op.isPresent()) {
                APlayer player = op.get();

                if(Anticheat.INSTANCE.getPacketHandler().processReceive(player, event.clone())) {
                    event.setCancelled(true);
                }
            }
        } catch (Throwable throwable) {
            Anticheat.INSTANCE.getLogger().log(Level.WARNING, "Error processing receive packet for user: " + user.getName(), throwable);
        }

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Handle packet send events here

        User user = event.getUser();

        if(user == null || user.getUUID() == null) {
            return;
        }

        try {
            Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(user.getUUID());


            if(op.isPresent()) {
                APlayer player = op.get();

                if(Anticheat.INSTANCE.getPacketHandler().processSend(player, event.clone())) {
                    Anticheat.INSTANCE.getLogger().info("Cancelled packet for user: " + user.getName() + " - " + event.getPacketType());
                    event.setCancelled(true);
                }
            }
        } catch (Throwable throwable) {
            Anticheat.INSTANCE.getLogger().log(Level.WARNING, "Error processing send packet for user: " + user.getName(), throwable);
        }
    }
}
