package dev.brighten.ac.check;

@FunctionalInterface
public interface WTimedAction<T> {
    void invoke(T event, long timestamp);
}
