package dev.brighten.ac.check;

@FunctionalInterface
public interface TimedWAction<T> {
    void invoke(T event, long timestamp);
}
