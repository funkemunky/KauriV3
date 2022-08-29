package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/*
   The whole purpose of this class is just to save disk space and make development more efficient
   with the use of lambdas and with less verbose conventions. This does not affect performance.
 */
public class RunUtils {

    public static BukkitTask taskTimer(Runnable runnable, long delay, long interval) {
        return Bukkit.getScheduler().runTaskTimer(Anticheat.INSTANCE.getPluginInstance(), runnable, delay, interval);
    }

    public static BukkitTask taskTimerAsync(Runnable runnable, long delay, long interval) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(Anticheat.INSTANCE.getPluginInstance(), runnable, delay, interval);
    }

    public static BukkitTask task(Runnable runnable) {
        return Bukkit.getScheduler().runTask(Anticheat.INSTANCE.getPluginInstance(), runnable);
    }

    public static BukkitTask taskAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(Anticheat.INSTANCE.getPluginInstance(), runnable);
    }

    public static BukkitTask taskLater(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(Anticheat.INSTANCE.getPluginInstance(), runnable, delay);
    }

    public static BukkitTask taskLaterAsync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(Anticheat.INSTANCE.getPluginInstance(), runnable, delay);
    }

    public static <T> Future<?> callLater(Future<T> runnable, long delay, Consumer<T> onComplete) {
        return Anticheat.INSTANCE.getScheduler().schedule(() -> {
            try {
                onComplete.accept(runnable.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<?> call(Future<T> runnable, Consumer<T> onComplete) {
        return Anticheat.INSTANCE.getScheduler().submit(() -> {
            try {
                onComplete.accept(runnable.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
