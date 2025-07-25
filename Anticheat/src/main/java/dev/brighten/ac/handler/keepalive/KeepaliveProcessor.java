package dev.brighten.ac.handler.keepalive;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.TransactionServerWrapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;

import java.util.Map;
import java.util.Optional;

public class KeepaliveProcessor {

    public KeepAlive currentKeepalive = new KeepAlive((short) 0);
    public short tick;
    public int totalPlayers, laggyPlayers;

    public final Map<Short, KeepAlive> keepAlives = new Short2ObjectArrayMap<>();

    final Int2ObjectMap<Short> lastResponses = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public KeepaliveProcessor() {
    }

    public void run() {
        tick++;

        if(tick > Short.MAX_VALUE - 2) {
            tick = 0;
            keepAlives.clear();
        }
        synchronized (keepAlives) {

            currentKeepalive = new KeepAlive(tick);
            keepAlives.put(currentKeepalive.id, currentKeepalive);
        }

        currentKeepalive.startStamp = System.currentTimeMillis();
        totalPlayers = laggyPlayers = 0;
        for (APlayer value : Anticheat.INSTANCE.getPlayerRegistry().aplayerMap.values()) {
            totalPlayers++;

            if (value.getLagInfo().getLastPingDrop().isNotPassed(2)
                    || value.getLagInfo().getLastClientTransaction().isPassed(135L)) laggyPlayers++;

            if (tick % 5 == 0) {
                double dh = Math.min(value.getMovement().getDeltaXZ(), 1),
                        dy = Math.min(1, Math.abs(value.getMovement().getDeltaY()));

                value.getInfo().nearbyEntities = value.getEntityLocationHandler().getTrackedEntities().values()
                        .stream()
                        .filter(te ->
                                te.getLocation().distance(value.getMovement().getTo().getLoc()) < (2 + (dh + dy) / 2))
                        .toList();
            }

            TransactionServerWrapper transaction = new TransactionServerWrapper(currentKeepalive.id, 0);

            value.writePacketSilently(transaction.getWrapper());
        }
    }

    public Optional<KeepAlive> getKeepById(short id) {
        return Optional.ofNullable(keepAlives.getOrDefault(id, null));
    }

    public Optional<KeepAlive> getResponse(APlayer data) {
        if (!lastResponses.containsKey(data.getBukkitPlayer().getUniqueId().hashCode()))
            return Optional.empty();

        return getKeepById(lastResponses.get(data.getBukkitPlayer().getUniqueId().hashCode()));
    }

    public void addResponse(APlayer data, short id) {
        getKeepById(id).ifPresent(ka -> {
            lastResponses.put(data.getBukkitPlayer().getUniqueId().hashCode(), (Short) id);
            ka.received(data);
        });
    }
}
