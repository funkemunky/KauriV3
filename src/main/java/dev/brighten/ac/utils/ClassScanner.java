package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import org.bukkit.Bukkit;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is copied from somewhere, can't remember from where though. Modified.
 * */
public class ClassScanner {
    private static final PathMatcher CLASS_FILE = create("glob:*.class");
    private static final PathMatcher ARCHIVE = create("glob:*.{jar}");

    public static Set<String> scanFile(String file, File f) {
        URL[] urls;
        try {
            urls = new URL[]{f.toURI().toURL()};
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
        return scanFile(file, urls);
    }

    public static Set<WrappedClass> getClasses(Class<? extends Annotation> annotationClass,
                                               String packageName) {
        return scanFile(annotationClass).stream().filter(pkg -> pkg.startsWith(packageName))
                .map(Reflections::getClass).collect(Collectors.toSet());
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

    public static Set<String> scanFile2(String file, Class<?> clazz) {
        Bukkit.getLogger().info("Found file: " + clazz.getProtectionDomain().getCodeSource().getLocation().toString());
        return scanFile2(file, new URL[]{clazz.getProtectionDomain().getCodeSource().getLocation()});
    }

    public static Set<String> scanFile2(String file, URL[] urls) {
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
                scanPath2(file, Paths.get(source), plugins);
            }
        }

        return plugins;
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

    private static void scanPath2(String file, Path path, Set<String> plugins) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                scanDirectory2(file, path, plugins);
            } else {
                scanZip2(file, path, plugins);
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

    private static void scanDirectory2(String file, Path dir, final Set<String> plugins) {
        try {
            Files.walkFileTree(dir, newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                            if (CLASS_FILE.matches(path.getFileName())) {
                                try (InputStream in = Files.newInputStream(path)) {
                                    String plugin = findClasses(file, in);
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

    private static void scanZip2(String file, Path path, Set<String> plugins) {
        if (!ARCHIVE.matches(path.getFileName())) {
            return;
        }

        try (ZipFile zip = new ZipFile(path.toFile())) {
            zip.stream().parallel().forEach(entry -> {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        String plugin = findClasses(file, in);
                        if (plugin != null) {
                            plugins.add(plugin);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
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