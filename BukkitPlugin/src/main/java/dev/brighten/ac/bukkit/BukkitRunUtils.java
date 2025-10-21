package dev.brighten.ac.bukkit;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.BukkitRunnable;
import dev.brighten.ac.utils.RunUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BukkitRunUtils implements RunUtils {
    public Object taskTimer(BukkitRunnable runnable, long delay, long interval) {
        AtomicReference<BukkitTask> task = new AtomicReference<>(null);

        task.set(Bukkit.getScheduler().runTaskTimer(AnticheatBukkit.INSTANCE,
                () -> runnable.run(task.get()), delay, interval));

        return task.get();
    }

    public Object taskTimerAsync(BukkitRunnable runnable, long delay, long interval) {
        AtomicReference<BukkitTask> task = new AtomicReference<>(null);

        task.set(Bukkit.getScheduler().runTaskTimerAsynchronously(AnticheatBukkit.INSTANCE,
                () -> runnable.run(task.get()), delay, interval));

        return task.get();
    }

    public Object task(Runnable runnable) {
        return Bukkit.getScheduler().runTask(AnticheatBukkit.INSTANCE, runnable);
    }

    public Object taskAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(AnticheatBukkit.INSTANCE, runnable);
    }

    public Object taskLater(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(AnticheatBukkit.INSTANCE, runnable, delay);
    }

    public Object taskLaterAsync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(AnticheatBukkit.INSTANCE, runnable, delay);
    }

    public <T> Future<?> callLater(Future<T> runnable, long delay, Consumer<T> onComplete) {
        return Anticheat.INSTANCE.getScheduler().schedule(() -> {
            try {
                onComplete.accept(runnable.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public <T> Future<?> call(Future<T> runnable, Consumer<T> onComplete) {
        return Anticheat.INSTANCE.getScheduler().submit(() -> {
            try {
                onComplete.accept(runnable.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
