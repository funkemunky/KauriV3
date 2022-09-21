package dev.brighten.ac.data.info;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.*;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.ActionStore;
import dev.brighten.ac.data.obj.CancellableActionStore;
import dev.brighten.ac.data.obj.TimedActionStore;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@RequiredArgsConstructor
public class CheckHandler {
    private final Map<Class<?>, ActionStore[]> events = new HashMap<>();
    private final Map<Class<?>, TimedActionStore[]> eventsWithTimestamp = new HashMap<>();
    private final Map<Class<?>, CancellableActionStore[]> cancellableEvents = new HashMap<>();

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

            synchronized (events) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getActions()) {
                    WAction<?> action = tuple.one.get(check);

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
            synchronized (eventsWithTimestamp) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getTimedActions()) {
                    WTimedAction<?> action = tuple.one.get(check);

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
            synchronized (cancellableEvents) {
                for (Tuple<WrappedField, Class<?>> tuple : checkClass.getCancellableActions()) {
                    WCancellable<?> action = tuple.one.get(check);

                    cancellableEvents.compute(tuple.two, (packetClass, array) -> {
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

    public void registerActions(Object object) {
        scanForActions(object.getClass());
    }

    private void scanForActions(Class<?> clazz) {
        WrappedClass wclass = new WrappedClass(clazz);
        for (WrappedField field : wclass.getFields()) {
            if(!WAction.class.isAssignableFrom(field.getType())
                    && !WCancellable.class.isAssignableFrom(field.getType())
                    && !WTimedAction.class.isAssignableFrom(field.getType())) continue;

            Type genericType = field.getField().getGenericType();
            Type type = null;

            if(genericType instanceof ParameterizedType) {
                type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            } else type = genericType;

            if(type == null) {
                Bukkit.getLogger().warning("Could not get type for field " + field.getField().getName()
                        + " in class " + clazz.getClass().getSimpleName());

                continue;
            }

            if(!Packet.class.isAssignableFrom((Class<?>) type)
                    && !WPacket.class.isAssignableFrom((Class<?>) type)
                    && !Event.class.isAssignableFrom((Class<?>) type)) {
                Bukkit.getLogger().warning("Type " + ((Class<?>) type).getSimpleName() + " is not a valid type for field "
                        + field.getField().getName() + " in class " + clazz.getClass().getSimpleName());
                continue;
            }

            if(field.getType().equals(WAction.class)) {
                WAction<?> action = field.get(clazz);

                synchronized (events) {
                    events.compute((Class<?>)type, (packetClass, array) -> {
                        if (array == null) {
                            return new ActionStore[] {new ActionStore(action, wclass.getParent())};
                        } else {
                            ActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new ActionStore(action, wclass.getParent());
                            return newArray;
                        }
                    });
                }
            } else if(field.getType().equals(WTimedAction.class)) { //This will always be TimedAction
                WTimedAction<?> action = field.get(clazz);

                synchronized (eventsWithTimestamp) {
                    eventsWithTimestamp.compute((Class<?>)type, (packetClass, array) -> {
                        if (array == null) {
                            return new TimedActionStore[] {new TimedActionStore(action, wclass.getParent())};
                        } else {
                            TimedActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new TimedActionStore(action, wclass.getParent());
                            return newArray;
                        }
                    });
                }
            } else if(field.getType().equals(WCancellable.class)) {
                WCancellable<?> action = field.get(clazz);
                synchronized (cancellableEvents) {
                    cancellableEvents.compute((Class<?>)type, (packetClass, array) -> {
                        if (array == null) {
                            return new CancellableActionStore[]
                                    {new CancellableActionStore(action, wclass.getParent())};
                        } else {
                            CancellableActionStore[] newArray = Arrays.copyOf(array, array.length + 1);
                            newArray[array.length] = new CancellableActionStore(action, wclass.getParent());
                            return newArray;
                        }
                    });
                }
            }
        }
    }

    public void shutdown() {
        checks.clear();
        events.clear();
        eventsWithTimestamp.clear();
        cancellableEvents.clear();
    }

    public void disableCheck(String checkName) {

    }

    public void callEvent(Event event) {
        if(events.containsKey(event.getClass())) {
            ActionStore<Event>[] actions = (ActionStore<Event>[]) events.get(event.getClass());
            for (ActionStore<Event> action : actions) {
                if(!Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass()).isEnabled())
                    continue;
                action.getAction().invoke(event);
            }
        }
    }

    //TODO When using WPacket wrappers only, make this strictly WPacket param based only

    public boolean callSyncPacket(Object packet, long timestamp) {
        if(events.containsKey(packet.getClass())) {
            synchronized (events) {
                ActionStore<Object>[] actions = events.get(packet.getClass());
                for (ActionStore<Object> action : actions) {
                    if(!Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass()).isEnabled())
                        continue;
                    action.getAction().invoke(packet);
                }
            }
        }
        if(eventsWithTimestamp.containsKey(packet.getClass())) {
            synchronized (eventsWithTimestamp) {
                TimedActionStore<Object>[] actions = eventsWithTimestamp.get(packet.getClass());
                for (TimedActionStore<Object> action : actions) {
                    if(!Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass()).isEnabled())
                        continue;
                    action.getAction().invoke(packet, timestamp);
                }
            }
        }
        if(cancellableEvents.containsKey(packet.getClass())) {
            boolean cancelled = false;
            synchronized (cancellableEvents) {
                CancellableActionStore<Object>[] actions = cancellableEvents.get(packet.getClass());
                for (CancellableActionStore<Object> action : actions) {
                    if(!Anticheat.INSTANCE.getCheckManager().getCheckSettings(action.getCheckClass()).isEnabled())
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
