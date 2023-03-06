package dev.brighten.ac.utils.objects;

import dev.brighten.ac.utils.Tuple;

@FunctionalInterface
public interface BiSupplier<T, V> {

    Tuple<T, V> get();
}
