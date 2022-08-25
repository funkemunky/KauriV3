package dev.brighten.ac.data.handlers;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.*;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.ActionStore;
import dev.brighten.ac.data.obj.CancellableActionStore;
import dev.brighten.ac.data.obj.TimedActionStore;
import dev.brighten.ac.handler.thread.ThreadHandler;
import dev.brighten.ac.utils.Async;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;

import java.util.*;

@RequiredArgsConstructor
public class CheckHandler {
    private final Map<Class<?>, ActionStore[]> events = new HashMap<>();
    private final Map<Class<?>, TimedActionStore[]> eventsWithTimestamp = new HashMap<>();
    private final Map<Class<?>, CancellableActionStore[]> cancellableEvents = new HashMap<>();

    private final Map<Class<?>, ActionStore[]> async_events = new HashMap<>();
    private final Map<Class<?>, TimedActionStore[]> async_eventsWithTimestamp = new HashMap<>();

    private final Map<Class<? extends Check>, Check> checkCache = new HashMap<>();

    private final List<Check> checks = new ArrayList<>();

    private final APlayer player;

    public synchronized Check findCheck(Class<? extends Check> checkClass) {
        return checkCache.computeIfAbsent(checkClass, key -> {
            for (Check check : checks) {
                if (check.getClass().equals(key)) {
                    return check;
                }
            }
            return null;
        });
    }

    public void initChecks() {
        // Enabling checks for players on join
        for (CheckStatic checkClass : Anticheat.INSTANCE.getCheckManager().getCheckClasses().values()) {
            CheckData data = checkClass.getCheckClass().getAnnotation(CheckData.class);

            //Version checks
            if(player.getPlayerVersion().isAbove(data.maxVersion()) || player.getPlayerVersion().isBelow(data.minVersion())) {
                Anticheat.INSTANCE.alog("Player " + player.getBukkitPlayer().getName() +
                        " is not on the right version for check " + data.name()
                        + " (version: " + player.getPlayerVersion().name() + ")");
                continue;
            }

            Check check = checkClass.playerInit(player);

            checks.add(check);

            synchronized (events) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getActions()) {
                    WAction<?> action = tuple.one.get(check);

                    if(!action.getClass().isAnnotationPresent(Async.class)) {
                        events.compute(tuple.two, (packetClass, array) -> {
                            if (array == null) {
                                return new ActionStore[] {new ActionStore(action, checkClass.getCheckClass().getParent())};
                            } else {
                                ActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                                newArray[array.length] = new ActionStore(action, checkClass.getCheckClass().getParent());
                                return newArray;
                            }
                        });
                    }
                }
            }
            synchronized (async_events) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getActions()) {
                    WAction<?> action = tuple.one.get(check);

                    if(action.getClass().isAnnotationPresent(Async.class)) {
                        async_events.compute(tuple.two, (packetClass, array) -> {
                            if (array == null) {
                                return new ActionStore[] {new ActionStore(action, checkClass.getCheckClass().getParent())};
                            } else {
                                ActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                                newArray[array.length] = new ActionStore(action, checkClass.getCheckClass().getParent());
                                return newArray;
                            }
                        });
                    }
                }
            }
            synchronized (eventsWithTimestamp) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getTimedActions()) {
                    WTimedAction<?> action = tuple.one.get(check);

                    if(!action.getClass().isAnnotationPresent(Async.class)) {
                        eventsWithTimestamp.compute(tuple.two, (packetClass, array) -> {
                            if (array == null) {
                                return new TimedActionStore[] {new TimedActionStore(action, checkClass.getCheckClass().getParent())};
                            } else {
                                TimedActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                                newArray[array.length] = new TimedActionStore(action, checkClass.getCheckClass().getParent());
                                return newArray;
                            }
                        });
                    }
                }
            }
            synchronized (async_eventsWithTimestamp) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getTimedActions()) {
                    WTimedAction<?> action = tuple.one.get(check);

                    if(action.getClass().isAnnotationPresent(Async.class)) {
                        async_eventsWithTimestamp.compute(tuple.two, (packetClass, array) -> {
                            if (array == null) {
                                return new TimedActionStore[] {new TimedActionStore(action, checkClass.getCheckClass().getParent())};
                            } else {
                                TimedActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                                newArray[array.length] = new TimedActionStore(action, checkClass.getCheckClass().getParent());
                                return newArray;
                            }
                        });
                    }
                }
            }
            synchronized (cancellableEvents) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getTimedActions()) {
                    WCancellable<?> action = tuple.one.get(check);

                    if(!action.getClass().isAnnotationPresent(Async.class)) {
                        cancellableEvents.compute(tuple.two, (packetClass, array) -> {
                            if (array == null) {
                                return new CancellableActionStore[] {new CancellableActionStore(action, checkClass.getCheckClass().getParent())};
                            } else {
                                CancellableActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                                newArray[array.length] = new CancellableActionStore(action, checkClass.getCheckClass().getParent());
                                return newArray;
                            }
                        });
                    } else {
                        Anticheat.INSTANCE.alog("WARNING: Async action " + action.getClass().getSimpleName() + " is not cancellable");
                    }
                }
            }
        }
    }

    public void shutdown() {
        checks.clear();
        events.clear();
        eventsWithTimestamp.clear();
        cancellableEvents.clear();
        async_events.clear();
        async_eventsWithTimestamp.clear();
    }

    public void disableCheck(String checkName) {

    }

    public void callEvent(Event event) {
        if(events.containsKey(event.getClass())) {
            ActionStore<Event>[] actions = (ActionStore<Event>[]) events.get(event.getClass());
            for (ActionStore<Event> action : actions) {
                action.getAction().invoke(event);
            }
        }
    }

    //TODO When using WPacket wrappers only, make this strictly WPacket param based only
    public void callPacket(Object packet, long timestamp) {
        ThreadHandler.INSTANCE.getThread(player).getThread().execute(() -> {
            if(async_events.containsKey(packet.getClass())) {
                synchronized (events) {
                    ActionStore<Object>[] actions = async_events.get(packet.getClass());
                    for (ActionStore<Object> action : actions) {
                        action.getAction().invoke(packet);
                    }
                }
            }
            if(async_eventsWithTimestamp.containsKey(packet.getClass())) {
                synchronized (events) {
                    TimedActionStore<Object>[] actions = async_eventsWithTimestamp.get(packet.getClass());
                    for (TimedActionStore<Object> action : actions) {
                        action.getAction().invoke(packet, timestamp);
                    }
                }
            }
        });
    }

    public boolean callSyncPacket(Object packet, long timestamp) {
        if(events.containsKey(packet.getClass())) {
            synchronized (events) {
                ActionStore<Object>[] actions = events.get(packet.getClass());
                for (ActionStore<Object> action : actions) {
                    action.getAction().invoke(packet);
                }
            }
        }
        if(eventsWithTimestamp.containsKey(packet.getClass())) {
            synchronized (events) {
                TimedActionStore<Object>[] actions = eventsWithTimestamp.get(packet.getClass());
                for (TimedActionStore<Object> action : actions) {
                    action.getAction().invoke(packet, timestamp);
                }
            }
        }
        if(cancellableEvents.containsKey(packet.getClass())) {
            boolean cancelled = false;
            synchronized (cancellableEvents) {
                CancellableActionStore<Object>[] actions = cancellableEvents.get(packet.getClass());
                for (CancellableActionStore<Object> action : actions) {
                    if(action.getAction().invoke(packet)) {
                        cancelled = true;
                    }
                }
            }
            return cancelled;
        }
        return false;
    }
}
