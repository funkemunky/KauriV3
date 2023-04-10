package dev.brighten.ac.check;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.WPacket;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CheckStatic {
    @Getter
    private final WrappedClass checkClass;
    private WrappedConstructor initConst;
    @Getter
    private final List<Tuple<WrappedField, Class<?>>> actions = new ArrayList<>(),
            timedActions = new ArrayList<>(), cancellableActions = new ArrayList<>();

    public CheckStatic(WrappedClass checkClass) {
        this.checkClass = checkClass;
        processClass();
    }

    private void processClass() {
        initConst = checkClass.getConstructor(APlayer.class);
        for (WrappedField field : checkClass.getFields()) {
            if(!field.isAnnotationPresent(Bind.class)) continue;

            if(!WAction.class.isAssignableFrom(field.getType())
                    && !WCancellable.class.isAssignableFrom(field.getType())
                    && !WTimedAction.class.isAssignableFrom(field.getType())) continue;

            Type genericType = field.getField().getGenericType();
            Type type = null;

            if(genericType instanceof ParameterizedType) {
                type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            } else type = genericType;

            if(type == null) {
                Bukkit.getLogger().warning("Could not get type for field " + field.getField().getName()
                        + " in class " + checkClass.getClass().getSimpleName());

                continue;
            }

            if(!Packet.class.isAssignableFrom((Class<?>) type)
                    && !WPacket.class.isAssignableFrom((Class<?>) type)
                    && !Event.class.isAssignableFrom((Class<?>) type)) {
                Bukkit.getLogger().warning("Type " + ((Class<?>) type).getSimpleName() + " is not a valid type for field "
                        + field.getField().getName() + " in class " + checkClass.getClass().getSimpleName());
                continue;
            }

            if(field.getType().equals(WAction.class)) {
                actions.add(new Tuple<>(field, (Class<?>)type));
            } else if(field.getType().equals(WTimedAction.class)) { //This will always be TimedAction
                timedActions.add(new Tuple<>(field, (Class<?>)type));
            } else if(field.getType().equals(WCancellable.class)) {
                cancellableActions.add(new Tuple<>(field, (Class<?>)type));
            }
        }
    }

    public <T> T playerInit(APlayer player) {
        return initConst.newInstance(player);
    }
}
