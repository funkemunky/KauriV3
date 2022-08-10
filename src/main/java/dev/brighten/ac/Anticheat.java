package dev.brighten.ac;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.brighten.ac.check.CheckManager;
import dev.brighten.ac.data.PlayerRegistry;
import dev.brighten.ac.handler.PacketHandler;
import dev.brighten.ac.handler.keepalive.KeepaliveProcessor;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.listener.PacketProcessor;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.math.RollingAverageDouble;
import dev.brighten.ac.utils.objects.RemoteClassLoader;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.WorldInfo;
import lombok.Getter;
import lombok.experimental.PackagePrivate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Init
public class Anticheat extends JavaPlugin {

    public static Anticheat INSTANCE;

    private ScheduledExecutorService scheduler;
    private PacketProcessor packetProcessor;
    private BukkitCommandManager commandManager;
    private CheckManager checkManager;
    private PlayerRegistry playerRegistry;
    private KeepaliveProcessor keepaliveProcessor;
    private PacketHandler packetHandler;
    private int currentTick;
    private Deque<Runnable> onTickEnd = new LinkedList<>();
    private ServerInjector injector;
    //Lag Information
    private Timer lastTickLag;
    private long lastTick;
    @PackagePrivate
    private RollingAverageDouble tps = new RollingAverageDouble(4, 20);
    private final Map<UUID, WorldInfo> worldInfoMap = new HashMap<>();

    @ConfigSetting(path = "logging", name = "verbose")
    private static boolean verboseLogging = true;

    public void onEnable() {
        INSTANCE = this;

        scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("Anticheat Schedular")
                .setUncaughtExceptionHandler((t, e) -> RunUtils.task(e::printStackTrace))
                .build());
        packetProcessor = new PacketProcessor();
        HandlerAbstract.init();

        commandManager = new BukkitCommandManager(this);
        commandManager.enableUnstableAPI("help");

        new CommandPropertiesManager(commandManager, getDataFolder(),
                getResource("command-messages.properties"));

        this.checkManager = new CheckManager();
        this.playerRegistry = new PlayerRegistry();
        this.keepaliveProcessor = new KeepaliveProcessor();
        this.packetHandler = new PacketHandler();

        alog(Color.Green + "Loading WorldInfo system...");
        Bukkit.getWorlds().forEach(w -> worldInfoMap.put(w.getUID(), new WorldInfo(w)));

        injector = new ServerInjector();
        try {
            injector.inject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeScanner(getClass(), this,
                null,
                true,
                true);
    }

    public void onDisable() {
        scheduler.shutdown();
        commandManager.unregisterCommands();

        // Unregistering packet listeners for players
        Bukkit.getOnlinePlayers().forEach(HandlerAbstract.getHandler()::remove);

        HandlerList.unregisterAll(this);
        packetProcessor.shutdown();
        checkManager = null;


        // Unregistering APlayer objects
        playerRegistry.unregisterAll();
        playerRegistry = null;
        try {
            injector.eject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader,
                                  boolean loadListeners, boolean loadCommands) {
        initializeScanner(mainClass, plugin, loader, ClassScanner.scanFile(null, mainClass), loadListeners,
                loadCommands);
    }

    public WorldInfo getWorldInfo(World world) {
        return worldInfoMap.computeIfAbsent(world.getUID(), key -> new WorldInfo(world));
    }

    public void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader, Set<String> names,
                                  boolean loadListeners, boolean loadCommands) {
        names.stream()
                .map(name -> {
                    if(loader != null) {
                        try {
                            if(loader instanceof RemoteClassLoader) {
                                return new WrappedClass(((RemoteClassLoader)loader).findClass(name));
                            } else
                                return new WrappedClass(Class.forName(name, true, loader));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        return null;
                    } else {
                        return Reflections.getClass(name);
                    }
                })
                .filter(c -> {
                    if(c == null) return false;
                    Init init = c.getAnnotation(Init.class);

                    String[] required = init.requirePlugins();

                    if(required.length > 0) {
                        if(init.requireType() == Init.RequireType.ALL) {
                            return Arrays.stream(required)
                                    .allMatch(name -> {
                                        if(name.contains("||")) {
                                            return Arrays.stream(name.split("\\|\\|"))
                                                    .anyMatch(n2 -> Bukkit.getPluginManager().isPluginEnabled(n2));
                                        } else if(name.contains("&&")) {
                                            return Arrays.stream(name.split("\\|\\|"))
                                                    .allMatch(n2 -> Bukkit.getPluginManager().isPluginEnabled(n2));
                                        } else return Bukkit.getPluginManager().isPluginEnabled(name);
                                    });
                        } else {
                            return Arrays.stream(required)
                                    .anyMatch(name -> {
                                        if(name.contains("||")) {
                                            return Arrays.stream(name.split("\\|\\|"))
                                                    .anyMatch(n2 -> Bukkit.getPluginManager().isPluginEnabled(n2));
                                        } else if(name.contains("&&")) {
                                            return Arrays.stream(name.split("\\|\\|"))
                                                    .allMatch(n2 -> Bukkit.getPluginManager().isPluginEnabled(n2));
                                        } else return Bukkit.getPluginManager().isPluginEnabled(name);
                                    });
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparing(c ->
                        c.getAnnotation(Init.class).priority().getPriority(), Comparator.reverseOrder()))
                .forEach(c -> {
                    Object obj = c.getParent().equals(mainClass) ? plugin : c.getConstructor().newInstance();
                    Init annotation = c.getAnnotation(Init.class);

                    if(loadListeners) {
                        if(obj instanceof Listener) {
                            Bukkit.getPluginManager().registerEvents((Listener)obj, plugin);
                            alog(true,"&7Registered Bukkit listener &e"
                                    + c.getParent().getSimpleName() + "&7.");
                        }
                    }

                    if(obj instanceof BaseCommand) {
                        alog(true,"&7Found BaseCommand for class &e"
                                + c.getParent().getSimpleName() + "&7! Registering commands...");
                        commandManager.registerCommand((BaseCommand)obj);
                    }

                    for (WrappedMethod method : c.getMethods()) {
                        if(method.getMethod().isAnnotationPresent(Invoke.class)) {
                            alog(true,"&7Invoking method &e" + method.getName() + " &7in &e"
                                    + c.getClass().getSimpleName() + "&7...");
                            method.invoke(obj);
                        }
                    }

                    for (WrappedField field : c.getFields()) {
                         if(field.isAnnotationPresent(ConfigSetting.class)) {
                            ConfigSetting setting = field.getAnnotation(ConfigSetting.class);

                            String name = setting.name().length() > 0
                                    ? setting.name()
                                    : field.getField().getName();

                            alog(true, "&7Found ConfigSetting &e%s &7(default=&f%s&7).",
                                    field.getField().getName(),
                                    (setting.hide() ? "HIDDEN" : field.get(obj)));


                            FileConfiguration config = plugin.getConfig();

                            if(config.get((setting.path().length() > 0 ? setting.path() + "." : "") + name) == null) {
                                alog(true,"&7Value not set in config! Setting value...");
                                config.set((setting.path().length() > 0 ? setting.path() + "." : "") + name, field.get(obj));
                                plugin.saveConfig();
                            } else {
                                Object configObj = config.get((setting.path().length() > 0 ? setting.path() + "." : "") + name);
                                alog(true, "&7Set field to value &e%s&7.",
                                        (setting.hide() ? "HIDDEN" : configObj));
                                field.set(obj, configObj);
                            }
                        }
                    }
                });
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
        }, this, 1L, 1L);
    }

    public void onTickEnd(Runnable runnable) {
        onTickEnd.add(runnable);
    }
}
