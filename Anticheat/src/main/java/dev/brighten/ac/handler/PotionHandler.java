package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.KPotionEffect;
import dev.brighten.ac.utils.KProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PotionHandler {
    private final APlayer data;

    private final List<KPotionEffect> potionEffects = new ArrayList<>();
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
            for (KPotionEffect effect : potionEffects) {
                lock.readLock().lock();

                try {
                    if (data.getBukkitPlayer().hasPositionEffect(effect.potionType())) continue;

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
                data.getPotionHandler().potionEffects.removeIf(pe -> pe.potionType().equals(type));
                data.getPotionHandler().potionEffects
                        .add(new KPotionEffect(type, new KProperties(packet.getEffectAmplifier()
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
            for (KPotionEffect potionEffect : potionEffects) {
                if (potionEffect.potionType().equals(type))
                    return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<KPotionEffect> getEffectByType(PotionType type) {
        lock.readLock().lock();
        try {
            for (KPotionEffect potionEffect : potionEffects) {
                if (potionEffect.potionType().equals(type))
                    return Optional.of(potionEffect);
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<KPotionEffect> getPotionEffects() {
        lock.readLock().lock();

        try {
            return new ArrayList<>(potionEffects);
        } finally {
            lock.readLock().unlock();
        }
    }
}