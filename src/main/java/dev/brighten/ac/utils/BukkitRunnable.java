package dev.brighten.ac.utils;

import org.bukkit.scheduler.BukkitTask;

@FunctionalInterface
public interface BukkitRunnable {
    void run(BukkitTask task);
}
