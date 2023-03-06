package dev.brighten.ac.data.obj;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.WTimedAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class TimedActionStore<T> {
    private final WTimedAction<T> action;
    private final Class<? extends Check> checkClass;
    //To ensure duplicate actions are not added to the list
    private final UUID uuid = UUID.randomUUID();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimedActionStore that = (TimedActionStore) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
