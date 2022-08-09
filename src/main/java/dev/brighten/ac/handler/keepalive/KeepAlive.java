package dev.brighten.ac.handler.keepalive;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KeepAlive {

    public final int start;
    public final short id;
    public long startStamp;

    public KeepAlive(int start, short id) {
        this.start = start;
        this.id = id;
    }

    public final Map<UUID, KAReceived> receivedKeepalive = new HashMap<>();

    public void received(APlayer player) {
        receivedKeepalive.put(player.getBukkitPlayer().getUniqueId(),
                new KAReceived(player, Anticheat.INSTANCE.getKeepaliveProcessor().tick));
    }

    public Optional<KAReceived> getReceived(UUID uuid) {
        return Optional.ofNullable(receivedKeepalive.getOrDefault(uuid, null));
    }

    @RequiredArgsConstructor
    public static class KAReceived {
        public final APlayer data;
        public final int stamp;
        public long receivedStamp;
    }
}