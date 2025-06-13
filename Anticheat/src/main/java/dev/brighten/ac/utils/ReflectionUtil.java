package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.logging.Level;

public final class ReflectionUtil {
    private static final Unsafe UNSAFE = ReflectionUtil.getUnsafeInstance();

    private static Unsafe getUnsafeInstance() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Anticheat.INSTANCE.getLogger().log(Level.SEVERE,"Could not locate Unsafe object!", e);
            return null;
        }
    }

    public static Field get(Class<?> oClass, Class<?> type, int index) throws NoSuchFieldException {
        int i = 0;
        for (Field field : oClass.getDeclaredFields()) {
            if (field.getType() == type) {
                if (i == index) {
                    field.setAccessible(true);
                    return field;
                }
                i++;
            }
        }

        throw new NoSuchFieldException("Could not find field of class " + type.getName() + " with index " + index);
    }

    public static <T> void setUnsafe(Object object, Field field, T value) {
        if(ReflectionUtil.UNSAFE == null) {
            Anticheat.INSTANCE.getLogger().severe("Unsafe is not initialized!");
            return;
        }
        ReflectionUtil.UNSAFE.putObject(object, ReflectionUtil.UNSAFE.objectFieldOffset(field), value);
    }
}