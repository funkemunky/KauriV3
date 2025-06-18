package dev.brighten.ac.handler.keepalive;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.BukkitRunnable;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;

public class KeepaliveProcessor implements BukkitRunnable {

    private BukkitTask task;

    public KeepAlive currentKeepalive = new KeepAlive((short) 0);
    public short tick;
    public int totalPlayers, laggyPlayers;

    public final Map<Short, KeepAlive> keepAlives = new Short2ObjectArrayMap<>();

    final Int2ObjectMap<Short> lastResponses = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public KeepaliveProcessor() {
    }

    @Override
    public void run(BukkitTask task) {
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

                value.getInfo().nearbyEntities = value.getBukkitPlayer()
                        .getNearbyEntities(2 + dh, 3 + dy, 2 + dh);
            }

            WrapperPlayServerPing transaction = new WrapperPlayServerPing(currentKeepalive.id);

            value.sendPacketSilently(transaction);
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

    public void start() {
        if (task == null) {
            task = Anticheat.INSTANCE.getRunUtils().taskTimer(this, 20L, 0L);
        }
    }

    public void addResponse(APlayer data, short id) {
        getKeepById(id).ifPresent(ka -> {
            lastResponses.put(data.getBukkitPlayer().getUniqueId().hashCode(), (Short) id);
            ka.received(data);
        });
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
