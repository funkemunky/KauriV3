package dev.brighten.ac.gui;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.menu.button.Button;
import dev.brighten.ac.utils.menu.type.impl.PagedMenu;
import org.apache.commons.lang.time.DateFormatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Logs extends PagedMenu {

    private final UUID uuid;
    private boolean generatedItems;
    private Set<String> allowedLogs = new HashSet<>();

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
        contents.clear();
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, check, 5000, 0, logs -> {
            if(allowedLogs.size() > 0) {
                Button button = new Button(false, new ItemBuilder(Material.REDSTONE).amount(1)
                        .name(Color.Red + "Stop Filtering").build(),
                        (player, info) -> {
                            allowedLogs.clear();
                            setCurrentPage(1);
                            getFixedItems().remove(getMenuDimension().getSize() - 5);
                            generateItems();
                        });
                getFixedItems().put(getMenuDimension().getSize() - 5, button);
            }
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

                addItem(new Button(false, stack, (player, info) -> {
                    switch(info.getClickType()) {
                        case SHIFT_LEFT: {
                            if(!allowedLogs.contains(log.getCheckId())) {
                                allowedLogs.add(log.getCheckId());
                                setCurrentPage(1);
                                generateItems(log.getCheckId());
                            } else {
                                allowedLogs.remove(log.getCheckId());
                                setCurrentPage(1);
                                generateItems(log.getCheckId());
                            }
                            break;
                        }
                    }
                }));
            }
            buildInventory(getHolder() == null);
            generatedItems = true;
        });
    }
    private void generateItems() {
        contents.clear();
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, logs -> {
            if(allowedLogs.size() > 0) {
                Button button = new Button(false, new ItemBuilder(Material.REDSTONE).amount(1)
                        .name(Color.Red + "Stop Filtering").build(),
                        (player, info) -> {
                    allowedLogs.clear();
                    setCurrentPage(1);
                    getFixedItems().remove(getMenuDimension().getSize() - 5);
                    generateItems();
                });
                getFixedItems().put(getMenuDimension().getSize() - 5, button);
            }
            for (Log log : logs) {
                if(allowedLogs.size() > 0 && !allowedLogs.contains(log.getCheckId())) continue;

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

                final String checkId = log.getCheckId();

                addItem(new Button(false, stack, (player, info) -> {
                    switch(info.getClickType()) {
                        case SHIFT_LEFT: {
                            if(!allowedLogs.contains(checkId)) {
                                allowedLogs.add(checkId);
                                player.sendMessage("Filtering" + checkId);
                                setCurrentPage(1);
                                generateItems(checkId);
                            } else {
                                allowedLogs.remove(checkId);
                                setCurrentPage(1);
                                generateItems(checkId);
                            }
                            break;
                        }
                    }
                }));
            }
            buildInventory(getHolder() == null);
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
