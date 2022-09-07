package dev.brighten.ac.handler.keepalive;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;

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

    public final Int2ObjectMap<KAReceived> receivedKeepalive = new Int2ObjectOpenHashMap<>(400);

    public void received(APlayer player) {
        receivedKeepalive.put(player.getBukkitPlayer().getUniqueId().hashCode(),
                new KAReceived(player, Anticheat.INSTANCE.getKeepaliveProcessor().tick));
    }

    public Optional<KAReceived> getReceived(UUID uuid) {
        int hashCode = uuid.hashCode();
        if(receivedKeepalive.containsKey(hashCode)) {
            return Optional.of(receivedKeepalive.get(hashCode));
        }

        return Optional.empty();
    }

    @RequiredArgsConstructor
    public static class KAReceived {
        public final APlayer data;
        public final int stamp;
        public long receivedStamp;
    }
}