package dev.brighten.ac.utils.objects;

@FunctionalInterface
public interface MultiFunction<R> {
    R apply(Object... o);
}
