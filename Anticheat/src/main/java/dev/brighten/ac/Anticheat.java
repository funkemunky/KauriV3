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
import dev.brighten.ac.depends.Relocate;
import dev.brighten.ac.handler.BBRevealHandler;
import dev.brighten.ac.handler.PacketHandler;
import dev.brighten.ac.handler.entity.FakeEntityTracker;
import dev.brighten.ac.handler.keepalive.KeepaliveProcessor;
import dev.brighten.ac.handler.keepalive.actions.ActionManager;
import dev.brighten.ac.logging.LoggerManager;
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
@MavenLibrary(groupId = "it\\.unimi\\.dsi", artifactId = "fastutil", version = "8.5.11", relocations = {
        @Relocate(from = "it\\.unimi", to = "dev.brighten.ac.libs.it.unimi")
})
@MavenLibrary(groupId = "org\\.yaml\\", artifactId = "snakeyaml", version = "2.2", relocations = {
        @Relocate(from = "org\\.yaml\\.snakeyaml", to = "dev.brighten.ac.libs.org.yaml.snakeyaml")
})
@MavenLibrary(groupId = "org\\.dizitart", artifactId = "nitrite", version = "4.3.0", relocations = {
        @Relocate(from = "org\\.dizitart", to = "dev.brighten.ac.libs.org.dizitart"),
        @Relocate(from = "org\\.h2", to = "dev.brighten.ac.libs.org.h2"),
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml"),
        @Relocate(from = "org\\.slf4j", to = "dev.brighten.ac.libs.org.slf4j"),
})
@MavenLibrary(groupId = "com.h2database", artifactId = "h2-mvstore", version = "2.2.224", relocations = {
        @Relocate(from = "org\\.h2", to = "dev.brighten.ac.libs.org.h2"),
})
@MavenLibrary(groupId = "com\\.\\fas\\terxml\\.jac\\kson\\.core", artifactId = "jackson-databind", version = "2.16.1", relocations = {
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml")
})
@MavenLibrary(groupId = "com\\.\\fas\\terxml\\.jac\\kson\\.core", artifactId = "jackson-core", version = "2.16.1", relocations = {
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml")
})
@MavenLibrary(groupId = "com\\.\\fas\\terxml\\.jac\\kson\\.core", artifactId = "jackson-annotations", version = "2.16.1", relocations = {
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml")
})
@MavenLibrary(groupId = "org\\.dizitart", artifactId = "nitrite-mvstore-adapter", version = "4.3.0", relocations = {
        @Relocate(from = "org\\.dizitart", to = "dev.brighten.ac.libs.org.dizitart"),
        @Relocate(from = "org\\.h2", to = "dev.brighten.ac.libs.org.h2"),
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml"),
        @Relocate(from = "org\\.slf4j", to = "dev.brighten.ac.libs.org.slf4j"),
})
@MavenLibrary(groupId = "org\\.slf4j", artifactId = "slf4j-api", version = "2.0.13", relocations = {
        @Relocate(from = "org\\.slf4j", to = "dev.brighten.ac.libs.org.slf4j"),
})
@MavenLibrary(groupId = "org\\.dizitart", artifactId = "nitrite-jackson-mapper", version = "4.3.0", relocations = {
        @Relocate(from = "org\\.dizitart", to = "dev.brighten.ac.libs.org.dizitart"),
        @Relocate(from = "com\\.fasterxml", to = "dev.brighten.ac.libs.com.fasterxml"),
        @Relocate(from = "org\\.slf4j", to = "dev.brighten.ac.libs.org.slf4j"),
})
public class Anticheat extends JavaPlugin {

    public static Anticheat INSTANCE;

    private ScheduledExecutorService scheduler;
    private BukkitCommandManager commandManager;
    private ActionManager actionManager;
    private CheckManager checkManager;
    private PlayerRegistry playerRegistry;
    private KeepaliveProcessor keepaliveProcessor;
    private PacketHandler packetHandler;
    private LoggerManager logManager;
    private CommandPropertiesManager commandPropertiesManager;
    private RunUtils runUtils;
    private ServerInjector serverInjector;

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
    private static boolean verboseLogging = true;

    private Configuration anticheatConfig;

    @Override
    public void onLoad() {
        INSTANCE = this;
        getLogger().info("Loading Anticheat...");
        LibraryLoader.loadAll(INSTANCE);

        PacketEventsRegister.register();
    }

    @SuppressWarnings("deprecation")
    public void onEnable() {

        runUtils = new RunUtils();

        scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("Anticheat Schedular")
                .setUncaughtExceptionHandler((t, e) -> {
                    Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Error in scheduler thread!", e);
                    Anticheat.INSTANCE.getRunUtils().task(e::printStackTrace);
                })
                .build());

        loadConfig();

        PacketEventsRegister.init();

        commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");

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

        new AnticheatAPI();

        new ClassScanner().initializeScanner(getClass(), this,
                null);

        if(!getAnticheatConfig().contains("database.username")) {
            getAnticheatConfig().set("database.username", "dbuser");
        }
        if(!getAnticheatConfig().contains("database.password")) {
            getAnticheatConfig().set("database.password", UUID.randomUUID().toString());
        }

        serverInjector = new ServerInjector();
        serverInjector.inject();

        this.fakeTracker = new FakeEntityTracker();
        this.checkManager = new CheckManager();
        this.playerRegistry = new PlayerRegistry();

        this.keepaliveProcessor = new KeepaliveProcessor();

        Bukkit.getOnlinePlayers().forEach(playerRegistry::generate);
        this.packetHandler = new PacketHandler();
        logManager = new LoggerManager();
        this.actionManager = new ActionManager();

        logManager.init();

        alog(Color.Green + "Loading WorldInfo system...");
        Bukkit.getWorlds().forEach(w -> worldInfoMap.put(w.getUID(), new WorldInfo(w)));

        PacketEventsRegister.registerListener();
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

        try {
            serverInjector.eject();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Anticheat Server Injector failed to eject", e);
        }

        getLogger().info("Disabling the rest of Kauri...");

        checkManager.getCheckClasses().clear();
        Check.alertsEnabled.clear();
        Check.debugInstances.clear();
        checkManager.getCheckSettings().clear();
        checkManager.getIdToName().clear();

        PacketEventsRegister.terminate();

        keepaliveProcessor.keepAlives.clear();


        logManager.shutDown();

        Bukkit.getScheduler().cancelTasks(this);

        fakeTracker.despawnAll();

        CheckHandler.TO_HOOK.clear();

        fakeTracker.despawnAll();

        worldInfoMap.clear();

        // Unregistering packet listeners for players
        HandlerList.unregisterAll(this);

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
