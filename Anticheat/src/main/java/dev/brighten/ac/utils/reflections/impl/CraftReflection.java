package dev.brighten.ac.utils.reflections.impl;

import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class CraftReflection {
    public static WrappedClass craftEntity = Reflections.getCBClass("entity.CraftEntity"); //1.7-1.14
    public static WrappedClass craftServer = Reflections.getCBClass("CraftServer"); //1.7-1.14\

    //Vanilla Instances
    private static final WrappedMethod entityInstance = craftEntity.getMethod("getHandle"); //1.7-1.14
    private static final WrappedMethod mcServerInstance = craftServer.getMethod("getServer"); //1.7-1.14

    public static <T> T getEntity(Entity entity) {
        return entityInstance.invoke(entity);
    }

    public static <T> T getMinecraftServer() {
        return mcServerInstance.invoke(Bukkit.getServer());
    }
}