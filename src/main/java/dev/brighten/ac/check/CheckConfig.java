package dev.brighten.ac.check;

import dev.brighten.ac.utils.ConfigSetting;
import dev.brighten.ac.utils.Init;

import java.util.Arrays;
import java.util.List;

@Init
public class CheckConfig {

    @ConfigSetting(name = "punishments.commands")
    public static List<String> punishmentCommands = Arrays.asList("kick %player% Unfair Advantage (%check%)");
}
