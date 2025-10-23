package dev.brighten.ac.bukkit;

import dev.brighten.ac.utils.KPotionEffect;
import dev.brighten.ac.utils.KProperties;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

public class BukkitUtils {

    public static KPotionEffect convertBukkitPotionEffect(org.bukkit.potion.PotionEffect effect) {
        return new KPotionEffect(SpigotConversionUtil.fromBukkitPotionEffectType(effect.getType()),
                new KProperties(effect.getAmplifier(), effect.getDuration(), effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon(), null));
    }
}
