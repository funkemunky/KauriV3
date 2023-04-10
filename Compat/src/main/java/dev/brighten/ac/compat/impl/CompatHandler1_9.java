package dev.brighten.ac.compat.impl;

import dev.brighten.ac.compat.CompatHandler;
import org.bukkit.entity.Player;

public class CompatHandler1_9 extends CompatHandler {

    @Override
    public boolean isRiptiding(Player player) {
        return false;
    }

    @Override
    public boolean isGliding(Player player) {
        return player.isGliding();
    }
}
