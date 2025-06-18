package dev.brighten.ac.data.info;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.*;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.ActionStore;
import dev.brighten.ac.data.obj.CancellableActionStore;
import dev.brighten.ac.data.obj.TimedActionStore;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CheckHandler {
    private final Map<Class<?>, ActionStore<?>[]> EVENTS = new HashMap<>();
    private final Map<Class<?>, TimedActionStore<?>[]> EVENTS_TIMESTAMP = new HashMap<>();
    private final Map<Class<?>, CancellableActionStore<?>[]> EVENTS_CANCELLABLE = new HashMap<>();

    @Getter
    private final Timer alertCountReset = new TickTimer(), lastPunish = new TickTimer();
    @Getter
    private final AtomicInteger alertCount = new AtomicInteger(0);

    private final Map<Class<? extends Check>, Check> checkCache = new HashMap<>();

    private final List<Check> checks = new ArrayList<>();

    private final APlayer player;

    public static final List<CheckStatic> TO_HOOK = new ArrayList<>();

    static {
        for (WrappedClass aClass : new ClassScanner().getClasses(Hook.class)) {
            TO_HOOK.add(new CheckStatic(aClass));
        }
    }

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

        for (CheckStatic toHook : TO_HOOK) {
            KListener listener = toHook.playerInit(player);
            synchronized (EVENTS) {
                for (Tuple<WrappedField, Class<?>> tuple : toHook.getActions()) {
                    WAction<?> action = tuple.one.get(listener);

                    EVENTS.compute(tuple.two, (packetClass, array) -> {
                        if (array == null) {
                            return new ActionStore[] {new ActionStore(action, toHook.getCheckClass().getParent())};
                        } else {
                            ActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new ActionStore(action, toHook.getCheckClass().getParent());
                            return newArray;
                        }
                    });
                }
            }
            synchronized (EVENTS_TIMESTAMP) {
                for (Tuple<WrappedField, Class<?>> tuple : toHook.getTimedActions()) {
                    WTimedAction<?> action = tuple.one.get(listener);

                    EVENTS_TIMESTAMP.compute(tuple.two, (packetClass, array) -> {
                        if (array == null) {
                            return new TimedActionStore[] {new TimedActionStore(action, toHook.getCheckClass().getParent())};
                        } else {
                            TimedActionStore<?>[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new TimedActionStore(action, toHook.getCheckClass().getParent());
                            return newArray;
                        }
                    });
                }
            }
            synchronized (EVENTS_CANCELLABLE) {
                for (Tuple<WrappedField, Class<?>> tuple : toHook.getCancellableActions()) {
                    WCancellable<?> action = tuple.one.get(listener);

                    if(tuple.one.isAnnotationPresent(Pre.class)) continue;

                    EVENTS_CANCELLABLE.compute(tuple.two, (packetClass, array) -> {
                        if (array == null) {
                            return new CancellableActionStore[] {new CancellableActionStore(action, toHook.getCheckClass().getParent())};
                        } else {
                            CancellableActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new CancellableActionStore(action, toHook.getCheckClass().getParent());
                            return newArray;
                        }
                    });
                }
            }
        }

        for (CheckStatic checkClass : Anticheat.INSTANCE.getCheckManager().getCheckClasses().values()) {
            CheckData data = checkClass.getCheckClass().getAnnotation(CheckData.class);

            //Version checks
            if(player.getPlayerVersion().isNewerThan(data.maxVersion()) || player.getPlayerVersion().isOlderThan(data.minVersion())) {
                Anticheat.INSTANCE.alog("Player " + player.getBukkitPlayer().getName() +
                        " is not on the right version for check " + data.name()
                        + " (version: " + player.getPlayerVersion().name() + ")");
                continue;
            }

            Check check = checkClass.playerInit(player);

            CheckSettings settings = Anticheat.INSTANCE.getCheckManager()
                    .getCheckSettings(checkClass.getCheckClass().getParent());

            if(settings == null) {
                throw new RuntimeException("Settings for check" + check.getName() + " do not exist!");
            }

            check.setEnabled(settings.isEnabled());
            check.setPunishable(settings.isPunishable());
            check.setCancellable(settings.isCancellable());
            check.setPunishVl(settings.getPunishVl());

            checks.add(check);

            synchronized (EVENTS) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getActions()) {
                    WAction<?> action = tuple.one.get(check);

                    EVENTS.compute(tuple.two, (packetClass, array) -> {
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
            synchronized (EVENTS_TIMESTAMP) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getTimedActions()) {
                    WTimedAction<?> action = tuple.one.get(check);

                    EVENTS_TIMESTAMP.compute(tuple.two, (packetClass, array) -> {
                        if (array == null) {
                            return new TimedActionStore[] {new TimedActionStore(action, checkClass.getCheckClass().getParent())};
                        } else {
                            TimedActionStore<?>[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new TimedActionStore(action, checkClass.getCheckClass().getParent());
                            return newArray;
                        }
                    });
                }
            }
            synchronized (EVENTS_CANCELLABLE) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getCancellableActions()) {
                    WCancellable<?> action = tuple.one.get(check);

                    if(tuple.one.isAnnotationPresent(Pre.class)) continue;

                    EVENTS_CANCELLABLE.compute(tuple.two, (packetClass, array) -> {
                        if (array == null) {
                            return new CancellableActionStore[] {new CancellableActionStore(action, checkClass.getCheckClass().getParent())};
                        } else {
                            CancellableActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new CancellableActionStore(action, checkClass.getCheckClass().getParent());
                            return newArray;
                        }
                    });
                }
            }
        }
    }

    public void shutdown() {
        checks.clear();
        EVENTS.clear();
        EVENTS_TIMESTAMP.clear();
        EVENTS_CANCELLABLE.clear();
    }

    public void disableCheck(String checkName) {

    }

    private void handleCheckLoading(Check check) {

    }

    public void callEvent(Event event) {
        if(!player.isInitialized()) {
            return;
        }
        if(EVENTS.containsKey(event.getClass())) {
            ActionStore<Event>[] actions = (ActionStore<Event>[]) EVENTS.get(event.getClass());
            for (ActionStore<Event> action : actions) {
                var checkSettings = Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass());
                if(checkSettings != null && !Anticheat.INSTANCE.getCheckManager()
                        .getCheckSettings(action.getCheckClass()).isEnabled())
                    continue;

                action.getAction().invoke(event);
            }
        }
    }

    //TODO When using WPacket wrappers only, make this strictly WPacket param based only

    public boolean callSyncPacket(Object packet, long timestamp) {
        if(!player.isInitialized()) {
            return false;
        }
        if(EVENTS.containsKey(packet.getClass())) {
            synchronized (EVENTS) {
                ActionStore<Object>[] actions = (ActionStore<Object>[]) EVENTS.get(packet.getClass());
                for (ActionStore<Object> action : actions) {
                    var checkSettings = Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass());
                    if(checkSettings != null && !Anticheat.INSTANCE.getCheckManager()
                            .getCheckSettings(action.getCheckClass()).isEnabled())
                        continue;
                    action.getAction().invoke(packet);
                }
            }
        }
        if(EVENTS_TIMESTAMP.containsKey(packet.getClass())) {
            synchronized (EVENTS_TIMESTAMP) {
                TimedActionStore<Object>[] actions = (TimedActionStore<Object>[])
                        EVENTS_TIMESTAMP.get(packet.getClass());
                for (TimedActionStore<Object> action : actions) {
                    var checkSettings = Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass());
                    if(checkSettings != null && !Anticheat.INSTANCE.getCheckManager()
                            .getCheckSettings(action.getCheckClass()).isEnabled())
                        continue;
                    action.getAction().invoke(packet, timestamp);
                }
            }
        }
        if(EVENTS_CANCELLABLE.containsKey(packet.getClass())) {
            boolean cancelled = false;
            synchronized (EVENTS_CANCELLABLE) {
                CancellableActionStore<Object>[] actions = (CancellableActionStore<Object>[])
                        EVENTS_CANCELLABLE.get(packet.getClass());
                for (CancellableActionStore<Object> action : actions) {
                    var checkSettings = Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass());
                    if(checkSettings != null && !Anticheat.INSTANCE.getCheckManager()
                            .getCheckSettings(action.getCheckClass()).isEnabled())
                        continue;

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
