package dev.brighten.ac.check;

import dev.brighten.ac.utils.annotation.ConfigSetting;
import dev.brighten.ac.utils.annotation.Init;

import java.util.Arrays;
import java.util.List;

@Init
public class CheckConfig {

    @ConfigSetting(name = "punishments.commands")
    public static List<String> punishmentCommands = Arrays.asList("kick %player% Unfair Advantage (%check%)");

    @ConfigSetting(name = "alerts.clickCommand")
    public static String clickCommand = "tp %player%";
}
