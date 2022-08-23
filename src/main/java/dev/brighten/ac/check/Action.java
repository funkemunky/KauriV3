package dev.brighten.ac.check;

@FunctionalInterface
public interface Action<T> {
    void invoke(T event);
}
