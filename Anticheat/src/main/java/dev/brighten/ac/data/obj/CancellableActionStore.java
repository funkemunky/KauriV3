package dev.brighten.ac.data.obj;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.WCancellable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class CancellableActionStore<T> {
    private final WCancellable<T> action;
    private final Class<? extends Check> checkClass;
    private final String checkId;
    private final UUID uuid = UUID.randomUUID();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CancellableActionStore that = (CancellableActionStore) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
