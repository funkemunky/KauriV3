package dev.brighten.ac.api;

import dev.brighten.ac.api.event.AnticheatEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

@SuppressWarnings("unused")
public class AnticheatAPI {

    public static AnticheatAPI INSTANCE;
    private final Map<String, List<AnticheatEvent>> registeredEvents = new HashMap<>();
    public AnticheatAPI() {
        INSTANCE = this;
    }

    /** Registers an AnticheatEvent to be called when an anticheat action is performed.
     * This method associates the given plugin with the provided AnticheatEvent.
     * @param plugin org.bukkit.plugin.Plugin
     * @param event dev.brighten.ac.api.event.AnticheatEvent
     */
    public void registerEvent(Plugin plugin, AnticheatEvent event) {
        registeredEvents.compute(plugin.getName(), (key, list) -> {
            if(list == null) {
                list = new ArrayList<>();
            }

            list.add(event);
            return list;
        });
    }

    public void shutdown() {
        registeredEvents.clear();
        INSTANCE = null;
    }

    
    public void unregisterEvents(Plugin plugin) {
        registeredEvents.remove(plugin.getName());
    }

    public List<AnticheatEvent> getAllEvents() {
        final List<AnticheatEvent> allEvents = new ArrayList<>();

        synchronized (registeredEvents) {
            for (List<AnticheatEvent> events : registeredEvents.values()) {
                allEvents.addAll(events);
            }
        }

        allEvents.sort(Comparator.comparing(e -> e.priority().getSlot()));

        return allEvents;
    }

}
