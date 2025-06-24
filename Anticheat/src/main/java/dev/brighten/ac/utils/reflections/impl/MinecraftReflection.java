package dev.brighten.ac.utils.reflections.impl;

import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;

public class MinecraftReflection {
    public static WrappedClass minecraftServer = Reflections.getNMSClass("MinecraftServer");
}
