package dev.brighten.ac.data;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.spigot.impl.LegacyPlayer;
import dev.brighten.ac.api.spigot.impl.ModernPlayer;
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
import dev.brighten.ac.handler.protocolsupport.Protocol;
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
import me.hydro.emulator.collision.impl.*;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


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

    public Emulator EMULATOR;

    public int hitsToCancel;

    public final Map<Short, Tuple<InstantAction, Consumer<InstantAction>>> instantTransaction = new HashMap<>();
    public final List<NormalAction> keepAliveStamps = new ArrayList<>();
    public final List<String> sniffedPackets = new CopyOnWriteArrayList<>();
    public boolean sniffing;

    @Getter
    private dev.brighten.ac.api.spigot.Player wrappedPlayer;

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
    @Getter
    private boolean initialized = false;

    public APlayer(Player player) {
        this.bukkitPlayer = player;
        this.uuid = player.getUniqueId();
        this.playerConnection = MinecraftReflection.getPlayerConnection(player);

        Anticheat.INSTANCE.getLogger().info("Constructored " + player.getName());
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

        Anticheat.INSTANCE.getLogger().info("Loading " + getBukkitPlayer().getName());

        // Grabbing the protocol version of the player.
        Anticheat.INSTANCE.getScheduler().schedule(() -> {
            Anticheat.INSTANCE.getLogger().info("Attempting Getting player version for " + getBukkitPlayer().getName());
            int numVersion = Protocol.getProtocol().getPlayerVersion(getBukkitPlayer());

            playerVersion = ProtocolVersion.getVersion(numVersion);

            RunUtils.task(() -> checkHandler.initChecks());

            if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
                this.wrappedPlayer = new LegacyPlayer(getBukkitPlayer());
            } else this.wrappedPlayer = new ModernPlayer(getBukkitPlayer());

            EMULATOR = new Emulator(new DataSupplier() {
                @Override
                public List<AxisAlignedBB> getCollidingBoxes(AxisAlignedBB bb) {
                    SimpleCollisionBox sbc = new SimpleCollisionBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

                    // Greater than 20? We want to truncate to prevent huge processing cost
                    if(sbc.min().distanceSquared(sbc.max()) > 400) {
                        sbc.maxX = sbc.minX + Math.min(sbc.maxX - sbc.minX, 20);
                        sbc.maxY = sbc.minY + Math.max(sbc.maxY - sbc.minY, 20);
                        sbc.maxZ = sbc.minZ + Math.max(sbc.maxZ - sbc.minZ, 20);
                    }

                    List<AxisAlignedBB> axisAlignedBBs = new ArrayList<>();

                    for (SimpleCollisionBox bb2 : Helper.getCollisions(APlayer.this,
                            sbc,
                            Materials.COLLIDABLE)) {
                       axisAlignedBBs
                               .add(new AxisAlignedBB(bb2.minX, bb2.minY, bb2.minZ, bb2.maxX, bb2.maxY, bb2.maxZ));
                    }

                    return axisAlignedBBs;
                }

                @Override
                public Block getBlockAt(BlockPos blockPos) {
                    //Optional<org.bukkit.block.Block>
                    val block = BlockUtils.getBlockAsync(
                            new Location(getBukkitPlayer().getWorld(), blockPos.getX(), blockPos.getY(), blockPos.getZ()));

                    if (block.isPresent()) {
                        XMaterial xmaterial = XMaterial.matchXMaterial(block.get().getType());

                        switch (xmaterial) {
                            case SLIME_BLOCK: {
                                return new BlockSlime();
                            }
                            case SOUL_SAND: {
                                return new BlockSoulSand();
                            }
                            case COBWEB: {
                                return new BlockWeb();
                            }
                            case ICE:
                            case PACKED_ICE:
                            case FROSTED_ICE: {
                                return new BlockIce();
                            }
                            case BLUE_ICE: {
                                return new BlockBlueIce();
                            }
                        }
                    }
                    return new Block();
                }
            }, playerVersion.getVersion());
            initialized = true;
        }, 100L, TimeUnit.MILLISECONDS);

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
        this.checkHandler.shutdown();
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

    public double getEyeHeight() {
        if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)) {
            return getInfo().sneaking ? 1.27f : 1.62f;
        } else {
            return getInfo().sneaking ? 1.54f : 1.62f;
        }
    }

    public double getPreviousEyeHeight() {
        if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)) {
            return getInfo().lsneaking ? 1.27f : 1.62f;
        } else {
            return getInfo().lsneaking ? 1.54f : 1.62f;
        }
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
