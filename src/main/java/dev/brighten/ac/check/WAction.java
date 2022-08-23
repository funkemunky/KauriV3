package dev.brighten.ac.check;

@FunctionalInterface
public interface WAction<T> {
    void invoke(T event);
}
