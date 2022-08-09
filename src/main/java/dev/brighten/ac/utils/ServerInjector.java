package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.reflections.impl.CraftReflection;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ServerInjector {
    private static final Collection<String> TICKABLE_CLASS_NAMES = Arrays.asList("IUpdatePlayerListBox", "ITickable", "Runnable");
    private Field hookedField;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void inject() throws Exception {
        // Start end of tick injection
        Object server = CraftReflection.getMinecraftServer();
        Class<?> serverClass = MinecraftReflection.minecraftServer.getParent();

        // Inject our hooked list for end of tick
        for (Field field : serverClass.getDeclaredFields()) {
            try {
                if (field.getType().equals(List.class)) {
                    // Check if type parameters match one of the tickable class names used throughout different versions
                    Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (!ServerInjector.TICKABLE_CLASS_NAMES.contains(genericType.getSimpleName())) {
                        continue;
                    }

                    field.setAccessible(true);

                    // Use a list wrapper to check when the size method is called
                    HookedListWrapper<?> wrapper = new HookedListWrapper<Object>((List) field.get(server)) {
                        @Override
                        public void onSize() {
                            Runnable toRun = null;

                            while((toRun = Anticheat.INSTANCE.getOnTickEnd().poll()) != null) {
                                toRun.run();
                            }
                        }
                    };

                    ReflectionUtil.setUnsafe(server, field, wrapper);
                    this.hookedField = field;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void eject() throws Exception {
        // Replace hooked wrapper with original
        if (this.hookedField != null) {
            Object server = CraftReflection.getMinecraftServer();

            HookedListWrapper<?> hookedListWrapper = (HookedListWrapper<?>) this.hookedField.get(server);

            ReflectionUtil.setUnsafe(server, this.hookedField, hookedListWrapper.getBase());
            this.hookedField = null;
        }
    }
}