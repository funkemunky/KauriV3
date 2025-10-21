package dev.brighten.ac.utils;

import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
public class Tuple<A, B> {
    public A one;
    public B two;

    public Tuple(A one, B two) {
        this.one = one;
        this.two = two;
    }

    public boolean equals(Object object) {
        if (this.getClass().isInstance(object)) {
            var toCompare = (Tuple<?, ?>) object;
            return one.equals(toCompare.one) && two.equals(toCompare.two);
        } else return false;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "one=" + one +
                ", two=" + two +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(one, two);
    }
}