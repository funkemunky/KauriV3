package dev.brighten.ac.check;

@FunctionalInterface
public interface TimedAction<T> {
    void invoke(T event, long timestamp);
}
