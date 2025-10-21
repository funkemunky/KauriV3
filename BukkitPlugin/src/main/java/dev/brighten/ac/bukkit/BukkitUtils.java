package dev.brighten.ac.bukkit;

import com.github.retrooper.packetevents.protocol.potion.PotionEffect;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import dev.brighten.ac.utils.Tuple;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

public class BukkitUtils {

    public static Tuple<PotionType, PotionEffect.Properties> convertBukkitPotionEffect(org.bukkit.potion.PotionEffect effect) {
        return new Tuple<>(SpigotConversionUtil.fromBukkitPotionEffectType(effect.getType()),
                new PotionEffect.Properties(effect.getAmplifier(), effect.getDuration(), effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon(), null));
    }
}
