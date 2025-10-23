package dev.brighten.ac.bukkit.menu.preset;

import dev.brighten.ac.bukkit.menu.preset.button.SettingButton;
import dev.brighten.ac.bukkit.menu.type.impl.ChestMenu;
import me.hydro.emulator.util.mcp.MathHelper;

public class SettingsMenu extends ChestMenu {
    public SettingsMenu(String title, SettingButton... buttons) {
        super(title, MathHelper.ceiling_float_int(buttons.length / 9f));

        for (int i = 0; i < buttons.length; i++) {
            setItem(i, buttons[i]);
        }
    }
}
