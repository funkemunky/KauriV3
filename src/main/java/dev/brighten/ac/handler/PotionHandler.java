package dev.brighten.ac.handler;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityEffect;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class PotionHandler {
    private final APlayer data;

    public List<PotionEffect> potionEffects = new CopyOnWriteArrayList<>();

    public void onFlying(WPacketPlayInFlying packet) {
        for (PotionEffect effect : potionEffects) {
            if(data.getBukkitPlayer().hasPotionEffect(effect.getType())) continue;

            data.runKeepaliveAction(d -> data.getPotionHandler().potionEffects.remove(effect));
        }
    }

    public void onPotionEffect(WPacketPlayOutEntityEffect packet) {
        data.runKeepaliveAction(d -> {
            val type = PotionEffectType.getById(packet.getEffectId());
            data.getPotionHandler().potionEffects.stream().filter(pe -> pe.getType().equals(type))
                    .forEach(data.getPotionHandler().potionEffects::remove);
            data.getPotionHandler().potionEffects
                    .add(new PotionEffect(type, packet.getDuration(), packet.getAmplifier(),
                            (packet.getFlags() & 1) == 1));
        });
    }

    public boolean hasPotionEffect(PotionEffectType type) {
        for (PotionEffect potionEffect : potionEffects) {
            if(potionEffect.getType().equals(type))
                return true;
        }
        return false;
    }

    public Optional<PotionEffect> getEffectByType(PotionEffectType type) {
        for (PotionEffect potionEffect : potionEffects) {
            if(potionEffect.getType().equals(type))
                return Optional.of(potionEffect);
        }
        return Optional.empty();
    }
}
