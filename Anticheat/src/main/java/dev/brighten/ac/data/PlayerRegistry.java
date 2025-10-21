package dev.brighten.ac.data;

import com.github.retrooper.packetevents.PacketEvents;
import dev.brighten.ac.api.KauriPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class PlayerRegistry {

    public final Int2ObjectMap<APlayer> aplayerMap = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public Optional<APlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(aplayerMap.get(uuid.hashCode()));
    }

    public APlayer generate(KauriPlayer player) {
        if(aplayerMap.containsKey(player.getUniqueId().hashCode())) {
            unregister(player.getUniqueId());
        }

        synchronized (aplayerMap) {
            APlayer aplayer = new APlayer(player, PacketEvents.getAPI().getPlayerManager().getUser(player));
            aplayerMap.put(player.getUniqueId().hashCode(), aplayer);
            return aplayer;
        }
    }

    public void unregister(UUID uuid) {
        synchronized (aplayerMap) {
            Optional.ofNullable(aplayerMap.remove(uuid.hashCode())).ifPresent(APlayer::unload);
        }
    }

    public void unregisterAll() {
        synchronized (aplayerMap) {
            aplayerMap.forEach((key, val) -> val.unload());
            aplayerMap.clear();
        }
    }
}
