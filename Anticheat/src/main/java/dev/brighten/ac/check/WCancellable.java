package dev.brighten.ac.check;

@FunctionalInterface
public interface WCancellable<T> {
    boolean invoke(T event);
}
