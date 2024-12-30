package dev.brighten.ac.data;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.CRC32;

public class PlayerRegistry {

    public final Int2ObjectMap<APlayer> aplayerMap = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public Optional<APlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(aplayerMap.get(uuid.hashCode()));
    }

    public APlayer generate(Player player) {
        if(aplayerMap.containsKey(player.getUniqueId().hashCode())) {
            unregister(player.getUniqueId());
        }

        synchronized (aplayerMap) {
            APlayer aplayer = new APlayer(player);
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
