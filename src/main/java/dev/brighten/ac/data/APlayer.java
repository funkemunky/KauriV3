package dev.brighten.ac.data;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.data.info.BlockInformation;
import dev.brighten.ac.data.info.CheckHandler;
import dev.brighten.ac.data.info.GeneralInformation;
import dev.brighten.ac.data.info.LagInformation;
import dev.brighten.ac.data.obj.InstantAction;
import dev.brighten.ac.data.obj.NormalAction;
import dev.brighten.ac.handler.EntityLocationHandler;
import dev.brighten.ac.handler.MovementHandler;
import dev.brighten.ac.handler.PotionHandler;
import dev.brighten.ac.handler.VelocityHandler;
import dev.brighten.ac.handler.block.BlockUpdateHandler;
import dev.brighten.ac.handler.entity.FakeMob;
import dev.brighten.ac.handler.keepalive.KeepAlive;
import dev.brighten.ac.handler.protocolsupport.ProtocolAPI;
import dev.brighten.ac.messages.Messages;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import me.hydro.emulator.Emulator;
import me.hydro.emulator.collision.Block;
import me.hydro.emulator.collision.impl.BlockSlime;
import me.hydro.emulator.collision.impl.BlockSoulSand;
import me.hydro.emulator.collision.impl.BlockWeb;
import me.hydro.emulator.object.input.DataSupplier;
import me.hydro.emulator.util.mcp.AxisAlignedBB;
import me.hydro.emulator.util.mcp.BlockPos;
import net.minecraft.server.v1_8_R3.PacketPlayOutTransaction;
import org.bukkit.Achievement;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class APlayer {
    @Getter
    private final Player bukkitPlayer;
    @Getter
    private final UUID uuid;
    @Getter
    private MovementHandler movement;
    @Getter
    private PotionHandler potionHandler;

    @Getter
    private VelocityHandler velocityHandler;

    @Getter
    private EntityLocationHandler entityLocationHandler;

    @Getter
    private BlockUpdateHandler blockUpdateHandler;

    @Getter
    private CheckHandler checkHandler;

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

    public final Emulator EMULATOR = new Emulator(new DataSupplier() {
        @Override
        public List<AxisAlignedBB> getCollidingBoxes(AxisAlignedBB bb) {
            return Helper.getCollisions(APlayer.this,
                            new SimpleCollisionBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ),
                            Materials.COLLIDABLE).stream().map(bb2 ->
                            new AxisAlignedBB(bb2.minX, bb2.minY, bb2.minZ, bb2.maxX, bb2.maxY, bb2.maxZ))
                    .collect(Collectors.toList());
        }

        @Override
        public Block getBlockAt(BlockPos blockPos) {
            //Optional<org.bukkit.block.Block>
            val block = BlockUtils.getBlockAsync(
                    new Location(getBukkitPlayer().getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            if(block.isPresent()) {
                XMaterial xmaterial = XMaterial.matchXMaterial(block.get().getType());

                switch (xmaterial) {
                    case SLIME_BLOCK: {
                        return new BlockSlime();
                    }
                    case SOUL_SAND : {
                        return new BlockSoulSand();
                    }
                    case COBWEB: {
                        return new BlockWeb();
                    }
                }
            }
            return new Block();
        }
    });

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
    @Getter
    private FakeMob mob;

    @Setter
    @Getter
    private boolean sendingPackets;

    public APlayer(Player player) {
        this.bukkitPlayer = player;
        this.uuid = player.getUniqueId();
        this.playerConnection = MinecraftReflection.getPlayerConnection(player);

        load();
    }

    private void load() {
        this.movement = new MovementHandler(this);
        this.potionHandler = new PotionHandler(this);
        this.velocityHandler = new VelocityHandler(this);
        this.entityLocationHandler = new EntityLocationHandler(this);
        this.blockUpdateHandler = new BlockUpdateHandler(this);
        this.checkHandler = new CheckHandler(this);
        this.info = new GeneralInformation();
        this.lagInfo = new LagInformation();
        this.blockInfo = new BlockInformation(this);

        creation.reset();

        // Grabbing the protocol version of the player.
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            playerVersion = ProtocolVersion.getVersion(ProtocolAPI.INSTANCE.getPlayerVersion(getBukkitPlayer()));

            RunUtils.task(() -> {
                checkHandler.initChecks();
            });
        });

        // Removing inventory achievement
        getBukkitPlayer().removeAchievement(Achievement.OPEN_INVENTORY);

        // Enabling alerts for players on join if they have the permissions to
        if(getBukkitPlayer().hasPermission("anticheat.command.alerts")
                || getBukkitPlayer().hasPermission("anticheat.alerts")) {
            Check.alertsEnabled.add(getUuid());
            getBukkitPlayer().spigot().sendMessage(Messages.ALERTS_ON);
        }

        generateEntities();
    }

    private void generateEntities() {
        mob = new FakeMob(EntityType.MAGMA_CUBE);

        KLocation origin = getMovement().getTo().getLoc().clone().add(0, 1.7, 0);

        RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection().multiply(-1));

        Location loc1 = coll.collisionPoint(2).toLocation(getBukkitPlayer().getWorld());

        mob.spawn(true, loc1,
                new ArrayList<>(Collections.singletonList(
                        new WrappedWatchableObject(0, 16, (byte) 1))), this);
    }

    protected void unload() {
        this.info = null;
        this.lagInfo = null;
        this.movement = null;
        mob.despawn();
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

        HandlerAbstract.getHandler().sendPacketSilently(this, new PacketPlayOutTransaction(0, startId, false));

        short finalEndId = endId, finalStartId = startId;
        Anticheat.INSTANCE.onTickEnd(() -> {
            InstantAction endAction = new InstantAction(finalStartId, finalEndId, true);
            synchronized (instantTransaction) {
                instantTransaction.put(finalEndId, new Tuple<>(endAction, runnable));
            }

            HandlerAbstract.getHandler()
                    .sendPacketSilently(this, new PacketPlayOutTransaction(0, finalEndId, false));
        });
    }

    public void addPlayerTick() {
        playerTick++;
    }

    public void sendPacketSilently(Object packet) {
        if(sniffing) {
            sniffedPackets.add("(Silent) [" +  Anticheat.INSTANCE.getKeepaliveProcessor().tick + "] " +
                    "" + (packet instanceof WPacket ? ((WPacket)packet).getPacketType()
                    : HandlerAbstract.getPacketType(packet)) + ": " + packet);
        }
        HandlerAbstract.getHandler().sendPacketSilently(this, packet);
    }

    public void sendPacket(Object packet) {
        HandlerAbstract.getHandler().sendPacket(this, packet);
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
