package dev.brighten.ac.utils;

import co.aikar.commands.BaseCommand;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.annotation.ConfigSetting;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.annotation.Invoke;
import dev.brighten.ac.utils.config.Configuration;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is copied from somewhere, can't remember from where though. Modified.
 * */
public class ClassScanner {
    private static final PathMatcher CLASS_FILE = create("glob:*.class");
    private static final PathMatcher ARCHIVE = create("glob:*.{jar}");

    //TODO Get check classes too
    public static Set<WrappedClass> getClasses(Class<? extends Annotation> annotationClass,
                                               String packageName) {
        Map<String, byte[]> map = Anticheat.INSTANCE.getStuffs();
        Map<String, byte[]> loadedClasses = Anticheat.INSTANCE.getLoadedClasses();
        Set<WrappedClass> toReturn = new HashSet<>();

        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            boolean startsWith = entry.getKey().startsWith(packageName);
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(startsWith && hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }

        for (Map.Entry<String, byte[]> entry : loadedClasses.entrySet()) {
            boolean startsWith = entry.getKey().startsWith(packageName);
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(startsWith && hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }
        return toReturn;
    }

    private static WrappedMethod findClassMethod =
            new WrappedClass(Anticheat.INSTANCE.getClassLoader2().getClass()).getMethod("findClass", String.class);
    public static void initializeScanner(Class<?> mainClass, Plugin plugin, Set<String> names) {
        names.stream()
                .map(name -> {
                    return new WrappedClass(findClassMethod.invoke(Anticheat.INSTANCE.getClassLoader2(), name));
                })
                .filter(c -> {
                    if(c.getParent() == null) {
                        return false;
                    }

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
        Map<String, byte[]> map = Anticheat.INSTANCE.getStuffs();
        Map<String, byte[]> loadedClasses = Anticheat.INSTANCE.getLoadedClasses();
        Set<WrappedClass> toReturn = new HashSet<>();

        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }

        for (Map.Entry<String, byte[]> entry : loadedClasses.entrySet()) {
            boolean hasAnnotation = findClass(new ByteArrayInputStream(entry.getValue()), annotationClass) != null;

            if(hasAnnotation) {
                toReturn.add(Reflections.getClass(entry.getKey()));
            }
        }
        return toReturn;
    }

    public static Set<String> getNames() {
        Map<String, byte[]> map = new HashMap<>(Anticheat.INSTANCE.getStuffs());

        Set<String> nameSet = new HashSet<>();

        for (String loadedClass : Anticheat.INSTANCE.getLoadedClasses().keySet()) {
            InputStream stream = new ByteArrayInputStream(Anticheat.INSTANCE.getLoadedClasses().get(loadedClass));

            if(findClass(stream, Init.class) != null) {
                nameSet.add(loadedClass);
            }
        }

        map.keySet().stream().filter(n -> !n.endsWith(".yml")
                && !n.endsWith(".xml") && !n.endsWith(".") && !n.endsWith(".properties")
                && !n.contains("dev.brighten.ac.packet")
                && Character.isLetterOrDigit(n.charAt(n.length() - 1))
                && findClass(map.get(n), Init.class) != null).forEach(nameSet::add);

        return nameSet;
    }

    public static String findClass(byte[] array, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(array);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');

            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if (annotation.desc
                            .equals("L" + anName + ";")) {
                        return className;
                    }
                }
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Failed to scan");
            //e.printStackTrace();
        }
        return null;
    }

    public static String findClass(InputStream stream, Class<? extends Annotation> annotationClass) {
        try {
            ClassReader reader = new ClassReader(stream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String className = classNode.name.replace('/', '.');

            final String anName = annotationClass.getName().replace(".", "/");
            if (classNode.visibleAnnotations != null) {
                for (Object node : classNode.visibleAnnotations) {
                    AnnotationNode annotation = (AnnotationNode) node;
                    if (annotation.desc
                            .equals("L" + anName + ";")) {
                        return className;
                    }
                }
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Failed to scan");
            //e.printStackTrace();
        }
        return null;
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

    public static String findClasses(String file, InputStream in) {
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            return classNode.name.replace('/', '.');
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to scan: " + in.toString());
        }
        return null;
    }

    public static PathMatcher create(String pattern) {
        return FileSystems.getDefault().getPathMatcher(pattern);
    }
}