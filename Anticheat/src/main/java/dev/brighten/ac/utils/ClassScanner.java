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
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is copied from somewhere, can't remember from where though. Modified.
 * */
public class ClassScanner {
    private final PathMatcher CLASS_FILE = create("glob:*.class");
    private final PathMatcher ARCHIVE = create("glob:*.{jar}");

    public void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader) {
        initializeScanner(mainClass, plugin, loader, scanFile(null, mainClass));
    }

    public void initializeScanner(Class<? extends Plugin> mainClass, Plugin plugin, ClassLoader loader, Set<String> names) {
        names.stream()
                .map(name -> {
                    if(loader != null) {
                        try {
                            if(loader instanceof RemoteClassLoader) {
                                return new WrappedClass(((RemoteClassLoader)loader).findClass(name));
                            } else
                                return new WrappedClass(Class.forName(name, true, loader));
                        } catch (ClassNotFoundException e) {
                            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to load class " + name, e);
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
                    return true;
                })
                .sorted(Comparator.comparing(c ->
                        c.getAnnotation(Init.class).priority().getPriority(), Comparator.reverseOrder()))
                .forEach(c -> {
                    Object obj = c.getParent().equals(mainClass) ? plugin : c.getConstructor().newInstance();

                    if(obj instanceof Listener listener) {
                        Bukkit.getPluginManager().registerEvents(listener, plugin);
                        Anticheat.INSTANCE.alog(true,"&7Registered Bukkit listener &e"
                                + c.getParent().getSimpleName() + "&7.");
                    }

                    if(obj instanceof BaseCommand command) {
                        Anticheat.INSTANCE.alog(true,"&7Found BaseCommand for class &e"
                                + c.getParent().getSimpleName() + "&7! Registering commands...");
                        Anticheat.INSTANCE.getCommandManager().registerCommand(command);
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

                            String name = !setting.name().isEmpty()
                                    ? setting.name()
                                    : field.getField().getName();

                            Anticheat.INSTANCE.alog(true, "&7Found ConfigSetting &e%s &7(default=&f%s&7).",
                                    field.getField().getName(),
                                    (setting.hide() ? "HIDDEN" : field.get(obj)));


                            Configuration config = Anticheat.INSTANCE.getAnticheatConfig();

                            if(config.get((!setting.path().isEmpty() ? setting.path() + "." : "") + name) == null) {
                                Anticheat.INSTANCE.alog(true,"&7Value not set in config! Setting value...");
                                config.set((!setting.path().isEmpty() ? setting.path() + "." : "") + name, field.get(obj));
                                Anticheat.INSTANCE.saveConfig();
                            } else {
                                Object configObj = config.get((!setting.path().isEmpty() ? setting.path() + "." : "") + name);
                                Anticheat.INSTANCE.alog(true, "&7Set field to value &e%s&7.",
                                        (setting.hide() ? "HIDDEN" : configObj));
                                field.set(obj, configObj);
                            }
                        }
                    }
                });
    }

    public  Set<WrappedClass> getClasses(Class<? extends Annotation> annotationClass) {
        return scanFile(annotationClass).stream().map(Reflections::getClass).collect(Collectors.toSet());
    }

    public  Set<String> scanFile(Class<? extends Annotation> annotationClass) {
        return scanFile(annotationClass, new URL[]{Anticheat.class.getProtectionDomain().getCodeSource().getLocation()});
    }

    public  Set<String> scanFile(String file, Class<?> clazz) {
        return scanFile(file, new URL[]{clazz.getProtectionDomain().getCodeSource().getLocation()});
    }

    public  Set<String> scanFile(String file, URL[] urls) {
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

    private  void scanPath(String file, Path path, Set<String> plugins) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                scanDirectory(file, path, plugins);
            } else {
                scanZip(file, path, plugins);
            }
        }
    }

    private  void scanDirectory(String file, Path dir, final Set<String> plugins) {
        try {
            Files.walkFileTree(dir, newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path path,
                                                                  @NotNull BasicFileAttributes attrs) throws IOException {
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
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan dir: " + dir, e);
        }
    }

    private  void scanZip(String file, Path path, Set<String> plugins) {
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
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan zip: " + path + "/" + file, e);
        }
    }

    public  String findPlugin(String file, InputStream in) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');
            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode node : classNode.visibleAnnotations) {
                    if ((file == null && node.desc
                            .equals("L" + Init.class.getName().replace(".", "/") + ";"))
                            || (file != null && node.desc
                            .equals("L" + file.replace(".", "/") + ";")))
                        return className;
                }
            }
            if (classNode.superName != null && (classNode.superName.equals(file))) return className;
        } catch (IllegalArgumentException e) {
            if(!e.getMessage().contains("Unsupported")) {
                Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan: " + file, e);
            }
        }
        catch (Exception e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to find plugin: " + file, e);
        }
        return null;
    }



    public  Set<String> scanFile(Class<? extends Annotation> annotationClass, URL[] urls) {
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

    private  void scanPath(Path path, Class<? extends Annotation> annotationClass, Set<String> plugins) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                scanDirectory(path, annotationClass, plugins);
            } else {
                scanZip(path, annotationClass, plugins);
            }
        }
    }

    private  void scanDirectory(Path dir, Class<? extends Annotation> annotationClass, final Set<String> plugins) {
        try {
            Files.walkFileTree(dir, newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) throws IOException {
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
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan directory " + dir, e);
        }
    }


    public  String findClass(InputStream in, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');
            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode node : classNode.visibleAnnotations) {
                    if (node.desc
                            .equals("L" + anName + ";"))
                        return className;
                }
            }
        } catch (IllegalArgumentException e) {
            if(!e.getMessage().contains("Unsupported")) {
                Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan: " + in.toString(), e);
            }
        } catch (Exception e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan: " + in.toString(), e);
        }
        return null;
    }

    private  void scanZip(Path path, Class<? extends Annotation> annotationClass, Set<String> plugins) {
        if (!ARCHIVE.matches(path.getFileName())) {
            return;
        }

        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class") || entry.getName().contains("META-INF")) {
                    continue;
                }

                try (InputStream in = zip.getInputStream(entry)) {
                    String plugin = findClass(in, annotationClass);
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            if(!e.getMessage().contains("Unsupported")) {
                Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan: " + path, e);
            }
        } catch (IOException e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Failed to scan directory " + path, e);
        }
    }

    @SafeVarargs
    private  <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet<>();
        Collections.addAll(set, elements);
        return set;
    }

    public  PathMatcher create(String pattern) {
        return FileSystems.getDefault().getPathMatcher(pattern);
    }
}