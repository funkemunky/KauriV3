package dev.brighten.ac.data;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckStatic;
import dev.brighten.ac.data.handlers.BlockInformation;
import dev.brighten.ac.data.handlers.GeneralInformation;
import dev.brighten.ac.data.handlers.LagInformation;
import dev.brighten.ac.data.handlers.MovementHandler;
import dev.brighten.ac.data.obj.InstantAction;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.handler.PotionHandler;
import dev.brighten.ac.handler.keepalive.KeepAlive;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import lombok.Getter;
import lombok.val;
import net.minecraft.server.v1_8_R3.PacketPlayOutTransaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;


public class APlayer {
    @Getter
    private final Player bukkitPlayer;
    private final List<Check> checks = new ArrayList<>();
    @Getter
    private MovementHandler movement;
    @Getter
    private PotionHandler potionHandler;
    @Getter
    private GeneralInformation info;
    @Getter
    private LagInformation lagInfo;
    @Getter
    private BlockInformation blockInformation;
    @Getter
    private int playerTick;
    @Getter
    //TODO Actually grab real player version once finished implementing version grabber from Atlas
    private ProtocolVersion playerVersion = ProtocolVersion.V1_8_9;

    public final Map<Short, Tuple<InstantAction, Consumer<InstantAction>>> instantTransaction = new HashMap<>();
    public final List<NormalAction> keepAliveStamps = new ArrayList<>();

    public APlayer(Player player) {
        this.bukkitPlayer = player;

        load();
    }

    private void load() {
        for (CheckStatic check : Anticheat.INSTANCE.getCheckManager().getCheckClasses()) {
            checks.add(check.playerInit(this));
        }
        this.movement = new MovementHandler(this);
        this.potionHandler = new PotionHandler(this);
        this.info = new GeneralInformation();
        this.lagInfo = new LagInformation();
        this.blockInformation = new BlockInformation(this);
    }

    protected void unload() {
        checks.clear();
        this.info = null;
        this.lagInfo = null;
        this.movement = null;
    }

    public void callEvent(Event event) {
        for (Check check : checks) {
            WrappedMethod[] methods = Anticheat.INSTANCE.getCheckManager().getEvents()
                    .get(new Tuple<String, Class<?>>(check.getCheckData().name(), event.getClass()));

            if(methods != null) {
                for (WrappedMethod method : methods) {
                    method.invoke(check, event);
                }
            }
        }
    }

    //TODO When using WPacket wrappers only, make this strictly WPacket param based only
    public void callPacket(Object packet) {
        for (Check check : checks) {
            WrappedMethod[] methods = Anticheat.INSTANCE.getCheckManager().getEvents()
                    .get(new Tuple<String, Class<?>>(check.getCheckData().name(), packet.getClass()));

            if(methods != null) {
                
                for (WrappedMethod method : 
                        methods) {
                    method.invoke(check, packet);
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
        instantTransaction.put(startId, new Tuple<>(startAction, runnable));

        HandlerAbstract.getHandler().sendPacket(this, new PacketPlayOutTransaction(0, startId, false));

        short finalEndId = endId, finalStartId = startId;
        Anticheat.INSTANCE.onTickEnd(() -> {
            InstantAction endAction = new InstantAction(finalStartId, finalEndId, true);
            instantTransaction.put(finalEndId, new Tuple<>(endAction, runnable));

            HandlerAbstract.getHandler()
                    .sendPacket(this, new PacketPlayOutTransaction(0, finalEndId, false));
        });
    }

    public void addPlayerTick() {
        playerTick++;
    }
}
