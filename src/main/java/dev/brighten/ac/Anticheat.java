package dev.brighten.ac;

import co.aikar.commands.BukkitCommandManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.brighten.ac.api.AnticheatAPI;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckManager;
import dev.brighten.ac.data.PlayerRegistry;
import dev.brighten.ac.depends.LibraryLoader;
import dev.brighten.ac.depends.MavenLibrary;
import dev.brighten.ac.depends.Repository;
import dev.brighten.ac.handler.PacketHandler;
import dev.brighten.ac.handler.entity.FakeEntityTracker;
import dev.brighten.ac.handler.keepalive.KeepaliveProcessor;
import dev.brighten.ac.handler.protocolsupport.ProtocolAPI;
import dev.brighten.ac.logging.LoggerManager;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.listener.PacketProcessor;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.annotation.ConfigSetting;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.config.Configuration;
import dev.brighten.ac.utils.config.ConfigurationProvider;
import dev.brighten.ac.utils.config.YamlConfiguration;
import dev.brighten.ac.utils.math.RollingAverageDouble;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.WorldInfo;
import dev.brighten.loader.LoaderPlugin;
import lombok.Getter;
import lombok.experimental.PackagePrivate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Init
//@MavenLibrary(groupId = "co.aikar", artifactId = "acf-bukkit", version = "0.5.1", repo = @Repository(url = "https://nexus.funkemunky.cc/content/repositories/releases/"))
@MavenLibrary(groupId = "com.google.guava", artifactId = "guava", version = "21.0", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "it.unimi.dsi", artifactId = "fastutil", version = "8.5.6", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "org.ow2.asm", artifactId = "asm", version = "9.2", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "org.ow2.asm", artifactId = "asm-tree", version = "9.2", repo = @Repository(url = "https://repo1.maven.org/maven2"))
public class Anticheat extends LoaderPlugin {

    public static Anticheat INSTANCE;

    private ScheduledExecutorService scheduler;
    private PacketProcessor packetProcessor;
    private BukkitCommandManager commandManager;
    private CheckManager checkManager;
    private PlayerRegistry playerRegistry;
    private KeepaliveProcessor keepaliveProcessor;
    private PacketHandler packetHandler;
    private LoggerManager logManager;

    private FakeEntityTracker fakeTracker;
    private int currentTick;
    private Deque<Runnable> onTickEnd = new LinkedList<>();
    private ServerInjector injector;
    //Lag Information
    private Timer lastTickLag;
    private long lastTick;
    @PackagePrivate
    private RollingAverageDouble tps = new RollingAverageDouble(4, 20);
    private final Map<UUID, WorldInfo> worldInfoMap = new HashMap<>();

    public static boolean allowDebug = true;

    @ConfigSetting(path = "logging", name = "verbose")
    private static boolean verboseLogging = true;

    private WrappedMethod findClassMethod;
    private Configuration anticheatConfig;

    public void onEnable() {
        INSTANCE = this;
        LibraryLoader.loadAll(getClass());

        scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("Anticheat Schedular")
                .setUncaughtExceptionHandler((t, e) -> RunUtils.task(e::printStackTrace))
                .build());

        loadConfig();

        IntegrityCheck.checkIntegrity();

        commandManager = new BukkitCommandManager(getPluginInstance());
        commandManager.enableUnstableAPI("help");

        new CommandPropertiesManager(commandManager, getDataFolder(),
                getResource2("command-messages.properties"));

        packetProcessor = new PacketProcessor();

        new AnticheatAPI();

        ClassScanner.initializeScanner(getPluginInstance().getClass(), getPluginInstance(),
                ClassScanner.getNames());

        if(!getAnticheatConfig().contains("database.username")) {
            getAnticheatConfig().set("database.username", "dbuser");
        }
        if(!getAnticheatConfig().contains("database.password")) {
            getAnticheatConfig().set("database.password", UUID.randomUUID().toString());
        }

        this.keepaliveProcessor = new KeepaliveProcessor();
        this.checkManager = new CheckManager();
        this.playerRegistry = new PlayerRegistry();
        this.packetHandler = new PacketHandler();
        logManager = new LoggerManager();

        keepaliveProcessor.start();

        HandlerAbstract.init();

        logManager.init();

        alog(Color.Green + "Loading WorldInfo system...");
        Bukkit.getWorlds().forEach(w -> worldInfoMap.put(w.getUID(), new WorldInfo(w)));

        injector = new ServerInjector();
        try {
            injector.inject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        fakeTracker = new FakeEntityTracker();

        Bukkit.getOnlinePlayers().forEach(HandlerAbstract.getHandler()::add);
    }
    public void onDisable() {
        scheduler.shutdown();
        commandManager.unregisterCommands();

        // Unregistering packet listeners for players
        HandlerAbstract.getHandler().shutdown();
        HandlerList.unregisterAll(getPluginInstance());
        packetProcessor.shutdown();
        packetProcessor = null;
        checkManager.getCheckClasses().clear();
        Check.alertsEnabled.clear();
        Check.debugInstances.clear();
        checkManager = null;
        keepaliveProcessor.keepAlives.cleanUp();
        keepaliveProcessor = null;
        ProtocolAPI.INSTANCE = null;
        tps = null;

        fakeTracker.despawnAll();
        fakeTracker = null;

        logManager.shutDown();

        Bukkit.getScheduler().cancelTasks(getPluginInstance());


        // Unregistering APlayer objects
        playerRegistry.unregisterAll();
        playerRegistry = null;
        try {
            injector.eject();
            injector = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        AnticheatAPI.INSTANCE = null;

        onTickEnd.clear();
        onTickEnd = null;
        packetHandler = null;
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(getAnticheatConfig(), new File(getDataFolder().getPath() + File.separator + "anticheat.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {

            anticheatConfig = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder().getPath() + File.separator + "anticheat.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadConfig() {
        try {
            File configFile = new File(getDataFolder(), "anticheat.yml");

            if(!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                if(!configFile.createNewFile()) {
                    throw new RuntimeException("Could not create new anticheat.yml in plugin folder!" +
                            "Insufficient write permissions?");
                } else {
                    MiscUtils.copy(INSTANCE.getResource2("anticheat.yml"), configFile);
                }
            }
            anticheatConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch(IOException e) {
            throw new RuntimeException("Could not load \"anticheat.yml\"!", e);
        }
    }

    public WorldInfo getWorldInfo(World world) {
        return worldInfoMap.computeIfAbsent(world.getUID(), key -> new WorldInfo(world));
    }

    public void alog(String log, Object... values) {
        alog(false, log, values);
    }

    public void alog(boolean verbose, String log, Object... values) {
        if(!verbose || verboseLogging) {
            if(values.length > 0)
                MiscUtils.printToConsole(log, values);
            else MiscUtils.printToConsole(log);
        }
    }

    public double getTps() {
        return this.tps.getAverage();
    }

    public void runTpsTask() {
        lastTickLag = new TickTimer();
        AtomicInteger ticks = new AtomicInteger();
        AtomicLong lastTimeStamp = new AtomicLong(0);
        RunUtils.taskTimer(() -> {
            ticks.getAndIncrement();
            currentTick++;
            long currentTime = System.currentTimeMillis();

            if(currentTime - lastTick > 120) {
                lastTickLag.reset();
            }
            if(ticks.get() >= 10) {
                ticks.set(0);
                tps.add(500D / (currentTime - lastTimeStamp.get()) * 20);
                lastTimeStamp.set(currentTime);
            }
            lastTick = currentTime;
        }, 1L, 1L);
    }

    public void onTickEnd(Runnable runnable) {
        onTickEnd.add(runnable);
    }
}
