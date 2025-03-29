package dev.brighten.ac.handler.keepalive;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutTransaction;
import dev.brighten.ac.utils.BukkitRunnable;
import dev.brighten.ac.utils.RunUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class KeepaliveProcessor implements BukkitRunnable {

    private BukkitTask task;

    public KeepAlive currentKeepalive = new KeepAlive(0, (short) 0);
    public int tick;
    public int totalPlayers, laggyPlayers;

    public final Cache<Short, KeepAlive> keepAlives = CacheBuilder.newBuilder().concurrencyLevel(4)
            .expireAfterWrite(15, TimeUnit.SECONDS).build();

    final Int2ObjectMap<Short> lastResponses = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public KeepaliveProcessor() {
    }

    @Override
    public void run(BukkitTask task) {
        tick++;
        synchronized (keepAlives) {
            short id = (short) (tick > Short.MAX_VALUE ? tick % Short.MAX_VALUE : tick);

            //Ensuring we don't have any duplicate IDS

            currentKeepalive = new KeepAlive(tick, id);
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

            WPacketPlayOutTransaction transaction = WPacketPlayOutTransaction.builder().id(0)
                    .action(currentKeepalive.id).accept(false).build();

            HandlerAbstract.getHandler().sendPacketSilently(value.getBukkitPlayer(), transaction);
        }
    }

    public Optional<KeepAlive> getKeepByTick(int tick) {
        return keepAlives.asMap().values().stream().filter(ka -> ka.start == tick).findFirst();
    }

    public Optional<KeepAlive> getKeepById(short id) {
        return Optional.ofNullable(keepAlives.getIfPresent(id));
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
