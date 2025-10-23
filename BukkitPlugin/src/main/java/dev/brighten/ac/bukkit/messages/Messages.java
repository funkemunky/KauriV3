package dev.brighten.ac.bukkit.messages;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class Messages {
    public static BaseComponent[] NULL_APLAYER
            = new ComponentBuilder("Could not read player data!").color(ChatColor.RED).create();
    public static BaseComponent[] ALERTS_ON = new ComponentBuilder("Enabled your anticheat alerts.")
            .color(ChatColor.GREEN).create();
    public static BaseComponent[] ALERTS_OFF = new ComponentBuilder("Disabled your anticheat alerts.")
            .color(ChatColor.RED).create();
}
