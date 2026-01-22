package dev.brighten.ac.messages;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Messages {
    public static Component NULL_APLAYER
            = Component.text("Could not read player data!").color(NamedTextColor.RED)
    public static Component ALERTS_ON = Component.text("Enabled your anticheat alerts.")
            .color(NamedTextColor.GREEN);
    public static Component ALERTS_OFF = Component.text("Disabled your anticheat alerts.")
            .color(NamedTextColor.RED);
}
