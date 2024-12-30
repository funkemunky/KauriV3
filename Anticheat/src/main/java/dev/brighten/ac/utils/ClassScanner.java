package dev.brighten.ac.utils;

import co.aikar.commands.BaseCommand;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.annotation.ConfigSetting;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.annotation.Invoke;
import dev.brighten.ac.utils.config.Configuration;
import dev.brighten.ac.utils.objects.RemoteClassLoader;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import dev.brighten.ac.utils.world.WorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is copied from somewhere, can't remember from where though. Modified.
 * */
public class ClassScanner {
    private static final PathMatcher CLASS_FILE = create("glob:*.class");
    private static final PathMatcher ARCHIVE = create("glob:*.{jar}");

    public static void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader,
                                         boolean loadListeners, boolean loadCommands) {
        initializeScanner(mainClass, plugin, loader, ClassScanner.scanFile(null, mainClass), loadListeners,
                loadCommands);
    }

    public static void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader, Set<String> names,
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

                    if(obj instanceof Listener) {
                        Bukkit.getPluginManager().registerEvents((Listener)obj, plugin);
                        Anticheat.INSTANCE.alog(true,"&7Registered Bukkit listener &e"
                                + c.getParent().getSimpleName() + "&7.");
                    }

                    if(obj instanceof BaseCommand) {
                        Anticheat.INSTANCE.alog(true,"&7Found BaseCommand for class &e"
                                + c.getParent().getSimpleName() + "&7! Registering commands...");
                        Anticheat.INSTANCE.getCommandManager().registerCommand((BaseCommand)obj);
                    }

                    for (WrappedMethod method : c.getMethods()) {
                        if(method.getMethod().isAnnotationPresent(Invoke.class)) {
                            Anticheat.INSTANCE.alog(true,"&7Invoking method &e" + method.getName() + " &7in &e"
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

                            Anticheat.INSTANCE.alog(true, "&7Found ConfigSetting &e%s &7(default=&f%s&7).",
                                    field.getField().getName(),
                                    (setting.hide() ? "HIDDEN" : field.get(obj)));


                            Configuration config = Anticheat.INSTANCE.getAnticheatConfig();

                            if(config.get((setting.path().length() > 0 ? setting.path() + "." : "") + name) == null) {
                                Anticheat.INSTANCE.alog(true,"&7Value not set in config! Setting value...");
                                config.set((setting.path().length() > 0 ? setting.path() + "." : "") + name, field.get(obj));
                                Anticheat.INSTANCE.saveConfig();
                            } else {
                                Object configObj = config.get((setting.path().length() > 0 ? setting.path() + "." : "") + name);
                                Anticheat.INSTANCE.alog(true, "&7Set field to value &e%s&7.",
                                        (setting.hide() ? "HIDDEN" : configObj));
                                field.set(obj, configObj);
                            }
                        }
                    }
                });
    }

    public static Set<WrappedClass> getClasses(Class<? extends Annotation> annotationClass) {
        return scanFile(annotationClass).stream().map(Reflections::getClass).collect(Collectors.toSet());
    }

    public static Set<String> scanFile(Class<? extends Annotation> annotationClass) {
        return scanFile(annotationClass, new URL[]{Anticheat.class.getProtectionDomain().getCodeSource().getLocation()});
    }

    public static Set<String> scanFile(Class<? extends Annotation> annotationClass, URL[] urls) {
        Set<URI> sources =  new HashSet<>();
        Set<String> plugins =  new HashSet<>();


        for (URL url : urls) {
            if (!url.getProtocol().equals("file")) {
                continue;
            }

            URI source;
            try {
                source = url.toURI();
            } catch (URISyntaxException e) {
                continue;
            }

            if (sources.add(source)) {
                scanPath(Paths.get(source), annotationClass, plugins);
            }
        }

        return plugins;
    }

    private static void scanPath(Path path, Class<? extends Annotation> annotationClass, Set<String> plugins) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                scanDirectory(path, annotationClass, plugins);
            } else {
                scanZip(path, annotationClass, plugins);
            }
        }
    }

    private static void scanDirectory(Path dir, Class<? extends Annotation> annotationClass, final Set<String> plugins) {
        try {
            Files.walkFileTree(dir, newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                            if (CLASS_FILE.matches(path.getFileName())) {
                                try (InputStream in = Files.newInputStream(path)) {
                                    String plugin = findClass(in, annotationClass);
                                    if (plugin != null) {
                                        plugins.add(plugin);
                                    }
                                }
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String findClass(InputStream in, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');
            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if (annotation.desc
                            .equals("L" + anName + ";"))
                        return className;
                }
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Failed to scan: " + in.toString());
        }
        return null;
    }

    private static void scanZip(Path path, Class<? extends Annotation> annotationClass, Set<String> plugins) {
        if (!ARCHIVE.matches(path.getFileName())) {
            return;
        }

        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                try (InputStream in = zip.getInputStream(entry)) {
                    String plugin = findClass(in, annotationClass);
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> scanFile(String file, Class<?> clazz) {
        return scanFile(file, new URL[]{clazz.getProtectionDomain().getCodeSource().getLocation()});
    }

    public static Set<String> scanFile(String file, URL[] urls) {
        Set<URI> sources =  new HashSet<>();
        Set<String> plugins =  new HashSet<>();


        for (URL url : urls) {
            if (!url.getProtocol().equals("file")) {
                continue;
            }

            URI source;
            try {
                source = url.toURI();
            } catch (URISyntaxException e) {
                continue;
            }

            if (sources.add(source)) {
                scanPath(file, Paths.get(source), plugins);
            }
        }

        return plugins;
    }

    private static void scanPath(String file, Path path, Set<String> plugins) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                scanDirectory(file, path, plugins);
            } else {
                scanZip(file, path, plugins);
            }
        }
    }

    private static void scanDirectory(String file, Path dir, final Set<String> plugins) {
        try {
            Files.walkFileTree(dir, newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                            if (CLASS_FILE.matches(path.getFileName())) {
                                try (InputStream in = Files.newInputStream(path)) {
                                    String plugin = findPlugin(file, in);
                                    if (plugin != null) {
                                        plugins.add(plugin);
                                    }
                                }
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet<>();
        Collections.addAll(set, elements);
        return set;
    }


    private static void scanZip(String file, Path path, Set<String> plugins) {
        if (!ARCHIVE.matches(path.getFileName())) {
            return;
        }

        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                try (InputStream in = zip.getInputStream(entry)) {
                    String plugin = findPlugin(file, in);
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String findPlugin(String file, InputStream in) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if ((file == null && annotation.desc
                            .equals("L" + Init.class.getName().replace(".", "/") + ";"))
                            || (file != null && annotation.desc
                            .equals("L" + file.replace(".", "/") + ";")))
                        return className;
                }
            }
            if (classNode.superName != null && (classNode.superName.equals(file))) return className;
        } catch (Exception e) {
            //System.out.println("Failed to scan: " + in.toString());
        }
        return null;
    }

    public static PathMatcher create(String pattern) {
        return FileSystems.getDefault().getPathMatcher(pattern);
    }
}