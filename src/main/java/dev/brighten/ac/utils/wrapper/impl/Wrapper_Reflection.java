package dev.brighten.ac.utils.wrapper.impl;

import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import dev.brighten.ac.utils.wrapper.Wrapper;
import org.bukkit.Material;

public class Wrapper_Reflection extends Wrapper {

    private static WrappedClass blockClass = Reflections.getNMSClass("Block");
    private static WrappedMethod getById = blockClass.getMethodByType(blockClass.getParent(), 0);
    private static WrappedField fieldFriction = blockClass.getFieldByType(float.class, 3);

    @Override
    public float getFriction(Material material) {
        return fieldFriction.get(getById.invoke(material));
    }
}
