package dev.brighten.ac.api;

import dev.brighten.ac.api.event.AnticheatEvent;

import java.util.*;

@SuppressWarnings("unused")
public class AnticheatAPI {

    public static AnticheatAPI INSTANCE;
    private final Map<Integer, List<AnticheatEvent>> registeredEvents = new HashMap<>();
    public AnticheatAPI() {
        INSTANCE = this;
    }

    /** Registers an AnticheatEvent to be called when an anticheat action is performed.
     * This method associates the given plugin with the provided AnticheatEvent.
     * @param plugin org.bukkit.plugin.Plugin
     * @param event dev.brighten.ac.api.event.AnticheatEvent
     */
    public void registerEvent(Object plugin, AnticheatEvent event) {
        registeredEvents.compute(plugin.hashCode(), (key, list) -> {
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

    
    public void unregisterEvents(Object plugin) {
        registeredEvents.remove(plugin.hashCode());
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
