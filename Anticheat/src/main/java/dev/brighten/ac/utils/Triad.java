package dev.brighten.ac.utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
public class Triad<A, B, C> {
    public A first;
    public B second;
    public C third;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triad<?, ?, ?> triad = (Triad<?, ?, ?>) o;
        return Objects.equals(first, triad.first) && Objects.equals(second, triad.second) && Objects.equals(third, triad.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }
}
