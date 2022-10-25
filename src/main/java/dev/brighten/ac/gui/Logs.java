package dev.brighten.ac.gui;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.menu.button.Button;
import dev.brighten.ac.utils.menu.type.impl.PagedMenu;
import org.apache.commons.lang.time.DateFormatUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Logs extends PagedMenu {

    private final UUID uuid;
    private boolean generatedItems;

    public Logs(UUID uuid) {
        super(Color.Gold + MojangAPI.getUsername(uuid).orElse("Unknown Player") + "'s Logs", 6);

        this.uuid = uuid;

        generateItems();
    }

    public Logs(UUID uuid, String check) {
        super(Color.Gold + MojangAPI.getUsername(uuid).orElse("Unknown Player") + "'s Logs", 6);

        this.uuid = uuid;

        generateItems(check);
    }

    private void generateItems(String check) {
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, check, 5000, 0, logs -> {
            for (Log log : logs) {
                ItemStack stack = new ItemBuilder(XMaterial.PAPER.parseMaterial()).amount(1)
                        .name(Color.Gold + log.getCheckId()).lore("&eVL:" + log.getVl(), "&eData:" + log.getData(),
                                "&eTime:" + DateFormatUtils.ISO_DATETIME_FORMAT.format(log.getTime())).build();

                addItem(new Button(false, stack));
            }
            generatedItems = true;
        });
    }
    private void generateItems() {
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, logs -> {
            for (Log log : logs) {
                ItemBuilder builder = new ItemBuilder(XMaterial.PAPER.parseMaterial()).amount(1)
                        .name(Color.Gold + log.getCheckId());

                String[] split = MiscUtils.splitIntoLine(log.getData(), 45);
                List<String> lore = new ArrayList<>(Arrays.asList("&eVL: &f" + log.getVl(),
                        "&eTime: &f" + DateFormatUtils.ISO_DATETIME_FORMAT.format(log.getTime()),
                        "&eData: &f" + split[0]));

                for (int i = 1; i < split.length; i++) {
                    lore.add(Color.White + split[i]);
                }

                ItemStack stack = builder.lore(lore).build();

                addItem(new Button(false, stack));
            }
            generatedItems = true;
        });
    }

    @Override
    public void showMenu(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(generatedItems) {
                    super.cancel();
                    Logs.super.showMenu(player);
                }
            }
        }.runTaskTimer(Anticheat.INSTANCE.getPluginInstance(), 0L, 4L);
    }


}
