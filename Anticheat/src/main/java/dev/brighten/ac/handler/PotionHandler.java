package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.protocol.potion.PotionEffect;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PotionHandler {
    private final APlayer data;

    private final List<Tuple<PotionType, PotionEffect.Properties>> potionEffects = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PotionHandler(APlayer data) {
        this.data = data;

        lock.writeLock().lock();

        try {
            data.getBukkitPlayer().getActivePotionEffects().addAll(potionEffects);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void onFlying(WrapperPlayClientPlayerFlying packet) {
        lock.readLock().lock();
        try {
            for (Tuple<PotionType, PotionEffect.Properties> effect : potionEffects) {
                lock.readLock().lock();

                try {
                    if (data.getBukkitPlayer().hasPositionEffect(effect.one)) continue;

                    data.runKeepaliveAction(d -> {
                        lock.writeLock().lock();
                        try {
                            data.getPotionHandler().potionEffects.remove(effect);
                        } finally {
                            lock.writeLock().unlock();
                        }
                    });
                } finally {
                    lock.readLock().unlock();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void onPotionEffect(WrapperPlayServerEntityEffect packet) {
        data.runKeepaliveAction(d -> {
            var type = packet.getPotionType();
            lock.writeLock().lock();
            try {
                data.getPotionHandler().potionEffects.removeIf(pe -> pe.one.equals(type));
                data.getPotionHandler().potionEffects
                        .add(new Tuple<>(type, new PotionEffect.Properties(packet.getEffectAmplifier()
                                , packet.getEffectDurationTicks(),
                                packet.isAmbient(), packet.isVisible(), packet.isShowIcon(), null)));
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    public boolean hasPotionEffect(PotionType type) {
        lock.readLock().lock();
        try {
            for (Tuple<PotionType, PotionEffect.Properties> potionEffect : potionEffects) {
                if (potionEffect.one.equals(type))
                    return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Tuple<PotionType, PotionEffect.Properties>> getEffectByType(PotionType type) {
        lock.readLock().lock();
        try {
            for (Tuple<PotionType, PotionEffect.Properties> potionEffect : potionEffects) {
                if (potionEffect.one.equals(type))
                    return Optional.of(potionEffect);
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }
}