package dev.brighten.ac.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;

import java.util.Optional;

public class PacketListener implements com.github.retrooper.packetevents.event.PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle packet receive events here
        User user = event.getUser();

        Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(user.getUUID());


        if(op.isPresent()) {
            APlayer player = op.get();

            if(Anticheat.INSTANCE.getPacketHandler().processReceive(player, event)) {
                event.setCancelled(true);
            }
        }

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Handle packet send events here

        User user = event.getUser();

        Optional<APlayer> op = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(user.getUUID());


        if(op.isPresent()) {
            APlayer player = op.get();

            if(Anticheat.INSTANCE.getPacketHandler().processSend(player, event)) {
                event.setCancelled(true);
            }
        }
    }
}
