package dev.brighten.ac.data;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.*;
import dev.brighten.ac.data.handlers.BlockInformation;
import dev.brighten.ac.data.handlers.GeneralInformation;
import dev.brighten.ac.data.handlers.LagInformation;
import dev.brighten.ac.data.handlers.MovementHandler;
import dev.brighten.ac.data.obj.ActionStore;
import dev.brighten.ac.data.obj.InstantAction;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.data.obj.TimedActionStore;
import dev.brighten.ac.handler.EntityLocationHandler;
import dev.brighten.ac.handler.PotionHandler;
import dev.brighten.ac.handler.VelocityHandler;
import dev.brighten.ac.handler.block.BlockUpdateHandler;
import dev.brighten.ac.handler.keepalive.KeepAlive;
import dev.brighten.ac.handler.protocolsupport.ProtocolAPI;
import dev.brighten.ac.messages.Messages;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.minecraft.server.v1_8_R3.PacketPlayOutTransaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;


public class APlayer {
    @Getter
    private final Player bukkitPlayer;
    @Getter
    private final UUID uuid;
    private final List<Check> checks = new ArrayList<>();
    @Getter
    private  MovementHandler movement;
    @Getter
    private PotionHandler potionHandler;

    @Getter
    private VelocityHandler velocityHandler;

    @Getter
    private EntityLocationHandler entityLocationHandler;

    @Getter
    private BlockUpdateHandler blockUpdateHandler;

    @Getter
    private GeneralInformation info;
    @Getter
    private LagInformation lagInfo;
    @Getter
    private BlockInformation blockInfo;
    @Getter
    private int playerTick;
    @Getter
    private Timer creation = new MillisTimer();
    @Getter
    //TODO Actually grab real player version once finished implementing version grabber from Atlas
    private ProtocolVersion playerVersion = ProtocolVersion.UNKNOWN;
    @Getter
    private Object playerConnection;

    public int hitsToCancel;

    public final Map<Short, Tuple<InstantAction, Consumer<InstantAction>>> instantTransaction = new HashMap<>();
    public final List<NormalAction> keepAliveStamps = new ArrayList<>();
    public final List<String> sniffedPackets = new CopyOnWriteArrayList<>();
    public boolean sniffing;

    @Getter
    private final Deque<Object> packetQueue = new LinkedList<>();
    @Getter
    private final List<Consumer<Vector>> onVelocityTasks = new ArrayList<>();
    public final EvictingList<Tuple<KLocation, Double>> pastLocations = new EvictingList<>(20);

    private final Map<Class<?>, ActionStore[]> events = new HashMap<>();
    private final Map<Class<?>, TimedActionStore[]> eventsWithTimestamp = new HashMap<>();

    @Setter
    @Getter
    private boolean sendingPackets;

    public APlayer(Player player) {
        this.bukkitPlayer = player;
        this.uuid = player.getUniqueId();
        this.playerConnection = MinecraftReflection.getPlayerConnection(player);
        load();
    }

    private final Map<Class<? extends Check>, Check> checkCache = new HashMap<>();

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

    private void load() {
        synchronized (checks) {
            for (CheckStatic check : Anticheat.INSTANCE.getCheckManager().getCheckClasses().values()) {
                checks.add(check.playerInit(this));
            }
        }
        this.movement = new MovementHandler(this);
        this.potionHandler = new PotionHandler(this);
        this.velocityHandler = new VelocityHandler(this);
        this.entityLocationHandler = new EntityLocationHandler(this);
        this.blockUpdateHandler = new BlockUpdateHandler(this);
        this.info = new GeneralInformation();
        this.lagInfo = new LagInformation();
        this.blockInfo = new BlockInformation(this);

        // Grabbing the protocol version of the player.
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            playerVersion = ProtocolVersion.getVersion(ProtocolAPI.INSTANCE.getPlayerVersion(getBukkitPlayer()));

            // Enabling checks for players on join
            for (CheckStatic checkClass : Anticheat.INSTANCE.getCheckManager().getCheckClasses().values()) {
                CheckData data = checkClass.getCheckClass().getAnnotation(CheckData.class);

                //Version checks
                if(playerVersion.isAbove(data.maxVersion()) || playerVersion.isBelow(data.minVersion())) {
                    Anticheat.INSTANCE.alog("Player " + getBukkitPlayer().getName() +
                            " is not on the right version for check " + data.name()
                            + " (version: " + playerVersion.name() + ")");
                    continue;
                }

                Check check = checkClass.playerInit(this);

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
                        TimedWAction<?> action = tuple.one.get(check);

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
        });

        creation.reset();

        // Enabling alerts for players on join if they have the permissions to
        if(getBukkitPlayer().hasPermission("anticheat.command.alerts")
                || getBukkitPlayer().hasPermission("anticheat.alerts")) {
            Check.alertsEnabled.add(getUuid());
            getBukkitPlayer().spigot().sendMessage(Messages.ALERTS_ON);
        }
    }

    protected void unload() {
        checks.clear();
        this.info = null;
        this.lagInfo = null;
        this.movement = null;
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
    }


    public int runKeepaliveAction(Consumer<KeepAlive> action) {
        return runKeepaliveAction(action, 0);
    }

    public int runKeepaliveAction(Consumer<KeepAlive> action, int later) {
        int id = Anticheat.INSTANCE.getKeepaliveProcessor().currentKeepalive.start + later;

        keepAliveStamps.add(new NormalAction(id, action));

        return id;
    }

    public void onVelocity(Consumer<Vector> runnable) {
        onVelocityTasks.add(runnable);
    }


    public void runInstantAction(Consumer<InstantAction> runnable) {
        runInstantAction(runnable, false);
    }

    public void runInstantAction(Consumer<InstantAction> runnable, boolean flush) {
        short startId = (short) ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, Short.MAX_VALUE),
                endId = (short)(startId + 1);

        //Ensuring we don't have any duplicate IDS
        val map = Anticheat.INSTANCE.getKeepaliveProcessor().keepAlives.asMap();
        while (map.containsKey(startId)
                || map.containsKey(endId)) {
            startId = (short) ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
            endId = (short)(startId + (short)1);
        }

        InstantAction startAction = new InstantAction(startId, endId, false);
        synchronized (instantTransaction) {
            instantTransaction.put(startId, new Tuple<>(startAction, runnable));
        }

        HandlerAbstract.getHandler().sendPacket(this, new PacketPlayOutTransaction(0, startId, false));

        short finalEndId = endId, finalStartId = startId;
        Anticheat.INSTANCE.onTickEnd(() -> {
            InstantAction endAction = new InstantAction(finalStartId, finalEndId, true);
            synchronized (instantTransaction) {
                instantTransaction.put(finalEndId, new Tuple<>(endAction, runnable));
            }

            HandlerAbstract.getHandler()
                    .sendPacket(this, new PacketPlayOutTransaction(0, finalEndId, false));
        });
    }

    public void addPlayerTick() {
        playerTick++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        APlayer aPlayer = (APlayer) o;
        return Objects.equals(uuid, aPlayer.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
