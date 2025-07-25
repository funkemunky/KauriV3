/*
 * Created by Justin Heflin on 4/19/18 8:21 PM
 * Copyright (c) 2018.
 *
 * Can be redistributed non commercially as long as credit is given to original copyright owner.
 *
 * last modified: 4/19/18 7:22 PM
 */
package dev.brighten.ac.utils.reflections;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.objects.QuadFunction;
import dev.brighten.ac.utils.objects.TriFunction;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public class Reflections {
    private static final String craftBukkitString;
    private static final String netMinecraftServerString;
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    public static String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    public static String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "")
            .replace(".", "");
    private static Set<String> classNames;

    static {
        String[] split = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        String version = split.length > 3 ? split[3] : null;
        craftBukkitString = "org.bukkit.craftbukkit." + (version == null ? "" : version + ".");
        netMinecraftServerString = "net.minecraft.server." + (version == null ? "" : version + ".");
        Anticheat.INSTANCE.alog(true, "MinecraftServer: + " + netMinecraftServerString);

        Anticheat.INSTANCE.alog(true, "Loading class names...");


        try {
            classNames = new ClassScanner().scanFile2(null,
                            Class.forName("org.bukkit.craftbukkit.Main"))
                    .stream().filter(s -> s.contains("net.minecraft"))
                    .collect(Collectors.toSet());
        } catch(Exception e) {
            classNames = Collections.emptySet();
        }
    }

    public static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static WrappedClass getCBClass(String name) {
        return getClass(craftBukkitString + name);
    }

    @SneakyThrows
    public static WrappedClass getNMSClass(String name) {
        try {
            return getClass(netMinecraftServerString + name);
        } catch(Throwable e) {
            Pattern toTest = Pattern.compile("\\." + name.replace("$", ".") + "$");
            for (String className : classNames) {
                if(toTest.matcher(className).find()) {
                    Anticheat.INSTANCE.alog(true, "FOUND CLASS: " + className);
                    return getClass(className);
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

    public static WrappedClass getClass(String name) {
        try {
            return new WrappedClass(Class.forName(name));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new NullPointerException("Class" + name + " could not be found!");
        }
    }

    public static WrappedClass getUtilClass(String name) {
        return getClass((PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_8)
                ? "net.minecraft.util." : "") + name.replace("dev.brighten.ac.utils.", ""));
    }

    @SneakyThrows
    public static <T> T createMethodLambda(Method method) {
        if(!method.isAccessible()) return null;
        val handle = lookup.unreflect(method);
        Class<?> functionType;
        switch(method.getParameterCount()) {
            case 0:
                functionType = Function.class;
                break;
            case 1:
                functionType = BiFunction.class;
                break;
            case 2:
                functionType = TriFunction.class;
                break;
            case 3:
                functionType = QuadFunction.class;
            default:
                functionType = null;
                break;
        }

        if(functionType != null) {
            return (T) LambdaMetafactory.metafactory(lookup, "apply",
                    MethodType.methodType(functionType),
                    MethodType.methodType(method.getReturnType(), handle.type().parameterArray()),
                    handle, handle.type()).getTarget().invoke();
        }

        return null;
    }

    public static WrappedClass getClass(Class clazz) {
        return new WrappedClass(clazz);
    }
}
