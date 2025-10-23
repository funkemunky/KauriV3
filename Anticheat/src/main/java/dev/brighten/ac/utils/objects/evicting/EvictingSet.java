package dev.brighten.ac.utils.objects.evicting;

import lombok.Getter;

import java.util.*;

public class EvictingSet<E> extends AbstractSet<E> implements Set<E>, Cloneable {

    @Getter
    private final int fixedSize;
    private transient EvictingMap<E, Object> map;

    public EvictingSet(int fixedSize) {
        this.fixedSize = fixedSize;
        this.map = new EvictingMap<>(fixedSize);
    }
    private static final Object PRESENT = new Object();


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return  map.keySet().toArray(a);
    }

    @Override
    public boolean add(E k) {
        return map.put(k, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    public Object clone() {
        try {
            EvictingSet<E> newSet = (EvictingSet<E>) super.clone();
            newSet.map = (EvictingMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
