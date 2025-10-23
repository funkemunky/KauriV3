package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public record KProperties(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon,
                          @Nullable KProperties hiddenEffect) {

    public PotionEffect.Properties toProperties() {
        return new PotionEffect.Properties(amplifier, duration, ambient, showParticles, showIcon,
                hiddenEffect == null ? null : hiddenEffect.toProperties());
    }
}
