package dev.brighten.ac.check;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckStatic {
    @Getter
    private final WrappedClass checkClass;
    private WrappedConstructor initConst;
    @Getter
    private final Map<Class<?>, List<WrappedMethod>> events = new HashMap<>();

    public CheckStatic(WrappedClass checkClass) {
        this.checkClass = checkClass;
        processClass();
    }

    private void processClass() {
        initConst = checkClass.getConstructor(APlayer.class);
        for (WrappedMethod method : checkClass.getDeclaredMethods()) {
            if(!method.isAnnotationPresent(Action.class)
                    || method.getParameters().length == 0) continue;
            Class<?> type = method.getParameterTypes()[0];

            if(Packet.class.isAssignableFrom(type)
                    || WPacket.class.isAssignableFrom(type) || Event.class.isAssignableFrom(type)) {
                events.compute(type, (key, list) -> {
                    if(list == null) list = new ArrayList<>();

                    list.add(method);
                    return list;
                });
            }
        }
    }

    public Check playerInit(APlayer player) {
        return initConst.newInstance(player);
    }
}
