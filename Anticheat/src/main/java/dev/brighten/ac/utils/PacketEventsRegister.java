package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.listener.PacketListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public class PacketEventsRegister {

    public static void register() {
        PacketEvents.getAPI().load();
    }

    public static void init() {
        PacketEvents.getAPI().init();
    }

    public static void registerListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(), PacketListenerPriority.MONITOR);
    }

    public static void terminate() {
        PacketEvents.getAPI().terminate();
    }
}
