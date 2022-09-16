package dev.brighten.ac.packet.wrapper.objects;

import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.WObject;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WrappedWatchableObject extends WObject {
    private static String type = (ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_8_5))
            ? "WatchableObject" :
            (ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)
                    ? "DataWatcher$WatchableObject"
                    : "DataWatcher$Item");
    private static WrappedClass c = Reflections.getNMSClass(type), dwo;
    private static WrappedConstructor constructor = c.getConstructor(int.class, int.class, Object.class);

    private static WrappedField firstIntField, dataValueIdField, dataWatcherObjectField,
            dataWatcherObjectIdField, dataSerializerField, watchedObjectField, watchedField;

    private int objectType, dataValueId;
    private Object watchedObject, dataWatcherObject, serializer;
    private boolean watched;

    public WrappedWatchableObject(Object object) {
        super(object);
    }

    public WrappedWatchableObject(int objectType, int dataValueId, Object watchedObject) {
        super(constructor.newInstance(objectType, dataValueId, watchedObject));

        this.dataValueId = dataValueId;
        this.watchedObject = watchedObject;
    }

    @Override
    public void processVanilla() {
        if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
            objectType = fetch(firstIntField);
            dataValueId = fetch(dataValueIdField);
        } else {
            objectType = -1;
            dataWatcherObject = fetch(dataWatcherObjectField);
            dataValueId = dataWatcherObjectIdField.get(dataWatcherObject);
            serializer = dataSerializerField.get(dataWatcherObject);
        }
        watchedObject = fetch(watchedObjectField);
        watched = fetch(watchedField);
    }

    @Override
    public Object toVanillaObject() {
        return constructor.newInstance(objectType, dataValueId, watchedObject);
    }

    static {
        if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
            firstIntField = c.getFieldByType(int.class, 0);
            dataValueIdField = c.getFieldByType(int.class, 1);
            watchedObjectField = c.getFieldByType(Object.class, 0);
        } else {
            dwo = Reflections.getNMSClass("DataWatcherObject");
            dataWatcherObjectIdField = c.getFieldByType(Object.class, 0);
            watchedObjectField = c.getFieldByType(Object.class, 1);

            dataWatcherObjectIdField = c.getFieldByType(dwo.getParent(), 0);
            dataSerializerField = c.getFieldByType(dwo.getParent(), 1);
        }
        watchedField = c.getFieldByType(boolean.class, 0);
    }
}
