package dev.brighten.ac.packet.wrapper;

import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.Getter;

@Getter
public abstract class WObject {

    private final Object vanillaObject;
    public WObject(Object object) {
        this.vanillaObject = object;
        processVanilla();
    }

    public abstract void processVanilla();

    public abstract Object toVanillaObject();

    public <T> T fetch(WrappedField field) {
        return field.get(getVanillaObject());
    }
}
