package dev.brighten.ac.handler.compat.impl;

import dev.brighten.ac.handler.compat.CompatHandler;
import org.bukkit.entity.Player;

public class CompatHandler1_8 extends CompatHandler {

    @Override
    public boolean isRiptiding(Player player) {
        return false;
    }

    @Override
    public boolean isGliding(Player player) {
        return false;
    }
}
