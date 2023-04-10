package dev.brighten.ac.check;

import dev.brighten.ac.data.APlayer;

import java.util.Optional;

public class KListener {

    public final APlayer player;

    public KListener(APlayer player) {
        this.player = player;
    }

    public <T extends Check> Optional<T> find(Class<T> checkClass) {
        Check check = player.getCheckHandler().findCheck(checkClass);

        if(check != null) {
            return Optional.of(checkClass.cast(check));
        }

        return Optional.empty();
    }
}
