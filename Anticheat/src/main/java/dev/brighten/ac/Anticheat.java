package dev.brighten.ac;

import co.aikar.commands.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.brighten.ac.api.AnticheatAPI;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckManager;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.PlayerRegistry;
import dev.brighten.ac.data.info.CheckHandler;
import dev.brighten.ac.depends.LibraryLoader;
import dev.brighten.ac.depends.MavenLibrary;
import dev.brighten.ac.depends.Repository;
import dev.brighten.ac.handler.BBRevealHandler;
import dev.brighten.ac.handler.PacketHandler;
import dev.brighten.ac.handler.entity.FakeEntityTracker;
import dev.brighten.ac.handler.keepalive.KeepaliveProcessor;
import dev.brighten.ac.handler.keepalive.actions.ActionManager;
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
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.WorldInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.PackagePrivate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@Init
@MavenLibrary(groupId = "it.unimi.dsi", artifactId = "fastutil", version = "8.5.11", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "org.ow2.asm", artifactId = "asm", version = "9.4", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "org.ow2.asm", artifactId = "asm-tree", version = "9.4", repo = @Repository(url = "https://repo1.maven.org/maven2"))
@MavenLibrary(groupId = "org.dizitart", artifactId = "nitrite-jackson-mapper", version = "4.3.0",  repo = @Repository(url = "https://repo1.maven.org/maven2"))
public class Anticheat extends JavaPlugin {

    public static Anticheat INSTANCE;

    private ScheduledExecutorService scheduler;
    private PacketProcessor packetProcessor;
    private BukkitCommandManager commandManager;
    private ActionManager actionManager;
    private CheckManager checkManager;
    private PlayerRegistry playerRegistry;
    private KeepaliveProcessor keepaliveProcessor;
    private PacketHandler packetHandler;
    private LoggerManager logManager;
    private CommandPropertiesManager commandPropertiesManager;
    private RunUtils runUtils;

    private FakeEntityTracker fakeTracker;
    private int currentTick;
    private final Deque<Runnable> onTickEnd = new LinkedList<>();
    //Lag Information
    private Timer lastTickLag;
    private long lastTick;
    @PackagePrivate
    private final RollingAverageDouble tps = new RollingAverageDouble(4, 20);
    private final Map<UUID, WorldInfo> worldInfoMap = new HashMap<>();

    private final List<BaseCommand> commands = new ArrayList<>();

    public static boolean allowDebug = true;

    @ConfigSetting(path = "logging", name = "verbose")
    private static final boolean verboseLogging = true;

    private Configuration anticheatConfig;

    @SuppressWarnings("deprecation")
    public void onEnable() {
        INSTANCE = this;
        new LibraryLoader().loadAll(getClass());

        runUtils = new RunUtils();

        scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("Anticheat Schedular")
                .setUncaughtExceptionHandler((t, e) -> {
                    Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Error in scheduler thread!", e);
                    Anticheat.INSTANCE.getRunUtils().task(e::printStackTrace);
                })
                .build());

        loadConfig();

        commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");

        runTpsTask();

        Anticheat.INSTANCE.getCommandManager()
                .getCommandCompletions().registerCompletion("@checks", (c) -> Anticheat.INSTANCE.getCheckManager().getCheckClasses().keySet()
                .stream()  .sorted(Comparator.naturalOrder())
                .map(name -> name.replace(" ", "_")).collect(Collectors.toList()));

        Anticheat.INSTANCE.getCommandManager()
                .getCommandCompletions().registerCompletion("@checkIds", (c) -> Anticheat.INSTANCE.getCheckManager().getCheckClasses().values()
                .stream().map(s -> s.getCheckClass().getAnnotation(CheckData.class).checkId())
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList()));

        BukkitCommandContexts contexts = (BukkitCommandContexts) Anticheat.INSTANCE.getCommandManager()
                .getCommandContexts();

        contexts.registerOptionalContext(Integer.class, c -> {
            String arg = c.popFirstArg();

            if(arg == null) return null;
            try {
                return Integer.parseInt(arg);
            } catch(NumberFormatException e) {
                throw new InvalidCommandArgument(String.format(Color.Red
                        + "Argument \"%s\" is not an integer", arg));
            }
        });

        contexts.registerOptionalContext(APlayer.class, c -> {
            if(c.hasFlag("other")) {
                String arg = c.popFirstArg();

                Player onlinePlayer = Bukkit.getPlayer(arg);

                if(onlinePlayer != null) {
                    return Anticheat.INSTANCE.getPlayerRegistry().getPlayer(onlinePlayer.getUniqueId())
                            .orElse(null);
                } else return null;
            } else {
                CommandSender sender = c.getSender();

                if(sender instanceof Player) {
                    return Anticheat.INSTANCE.getPlayerRegistry().getPlayer(((Player) sender).getUniqueId())
                            .orElse(null);
                }
                else if(!c.isOptional()) throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE,
                        false);
                else return null;
            }
        });

        commandPropertiesManager = new CommandPropertiesManager(commandManager, getDataFolder(),
                getResource("command-messages.properties"));

        packetProcessor = new PacketProcessor();

        new AnticheatAPI();

        new ClassScanner().initializeScanner(getClass(), this,
                null, Collections.emptyNavigableSet());

        if(!getAnticheatConfig().contains("database.username")) {
            getAnticheatConfig().set("database.username", "dbuser");
        }
        if(!getAnticheatConfig().contains("database.password")) {
            getAnticheatConfig().set("database.password", UUID.randomUUID().toString());
        }


        this.keepaliveProcessor = new KeepaliveProcessor();
        this.fakeTracker = new FakeEntityTracker();
        this.checkManager = new CheckManager();
        this.playerRegistry = new PlayerRegistry();
        HandlerAbstract.init();
        Bukkit.getOnlinePlayers().forEach(playerRegistry::generate);
        this.packetHandler = new PacketHandler();
        logManager = new LoggerManager();
        this.actionManager = new ActionManager();

        Bukkit.getOnlinePlayers().forEach(player -> player.kickPlayer("Server restarting..."));


        keepaliveProcessor.start();

        logManager.init();

        alog(Color.Green + "Loading WorldInfo system...");
        Bukkit.getWorlds().forEach(w -> worldInfoMap.put(w.getUID(), new WorldInfo(w)));

        Bukkit.getOnlinePlayers().forEach(HandlerAbstract.getHandler()::add);
    }

    public void onDisable() {
        scheduler.shutdownNow();


        // Unregistering APlayer objects
        playerRegistry.unregisterAll();
        commands.forEach(commandManager::unregisterCommand);
        commandManager.unregisterCommands();
        commandManager.getCommandCompletions().unregisterCompletion("@checks");
        try {
            commandManager.getCommandCompletions().unregisterCompletion("@checkIds");
        } catch (IllegalStateException e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Check ID unregister failed", e);
        }
        commandManager.getScheduler().cancelLocaleTask();
        commandPropertiesManager = null;

        getLogger().info("Disabling the rest of Kauri...");

        checkManager.getCheckClasses().clear();
        Check.alertsEnabled.clear();
        Check.debugInstances.clear();
        checkManager.getCheckSettings().clear();
        checkManager.getIdToName().clear();

        keepaliveProcessor.stop();
        keepaliveProcessor.keepAlives.clear();


        logManager.shutDown();

        Bukkit.getScheduler().cancelTasks(this);

        fakeTracker.despawnAll();

        CheckHandler.TO_HOOK.clear();

        fakeTracker.despawnAll();

        worldInfoMap.clear();



        // Unregistering packet listeners for players
        HandlerAbstract.getHandler().shutdown();
        HandlerList.unregisterAll(this);
        packetProcessor.shutdown();


        onTickEnd.clear();


        AnticheatAPI.INSTANCE.shutdown();

        BBRevealHandler.INSTANCE = null;
        INSTANCE = null;

    }

    public void info(@Nonnull String s) {
        getLogger().info(s);
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(getAnticheatConfig(), new File(getDataFolder().getPath() + File.separator + "anticheat.yml"));
        } catch (IOException e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Anticheat config save failed", e);
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
                if(!configFile.getParentFile().mkdirs() && !configFile.createNewFile()) {
                    throw new RuntimeException("Could not create new anticheat.yml in plugin folder!" +
                            "Insufficient write permissions?");
                } else {
                    MiscUtils.copy(INSTANCE.getResource("anticheat.yml"), configFile);
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
        Anticheat.INSTANCE.getRunUtils().taskTimer(task -> {
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
