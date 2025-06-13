package dev.brighten.ac.gui;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.menu.button.Button;
import dev.brighten.ac.utils.menu.button.UpdatingButton;
import dev.brighten.ac.utils.menu.type.impl.ChestMenu;
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
    private final Set<String> allowedLogs = new HashSet<>();

    public Logs(UUID uuid) {
        this(uuid, 5000);
    }

    public Logs(UUID uuid, int limit) {
        super(Color.Gold + MojangAPI.getUsername(uuid).orElse("Unknown Player") + "'s Logs", 6);

        this.uuid = uuid;

        updateLogs(limit, this::generateItems);
    }

    public Logs(UUID uuid, String check) {
        this(uuid, 5000, check);
    }

    public Logs(UUID uuid, int limit, String check) {
        super(Color.Gold + MojangAPI.getUsername(uuid).orElse("Unknown Player") + "'s Logs", 6);

        this.uuid = uuid;

        updateLogs(check, limit, this::generateItems);
    }

    private final List<Log> logs = new ArrayList<>();

    private void showFilterMenu(Player player) {
        ChestMenu filterMenu = new ChestMenu(Color.Gold + "Filter Logs", 4);

        this.close(player);
        logs.stream().map(Log::getCheckId).distinct().forEach(check -> filterMenu
                .addItem(new UpdatingButton(false, (pl, info) -> {
            if(!allowedLogs.contains(check)) {
                allowedLogs.add(check);
                setCurrentPage(1);
                pl.sendMessage("Filtering" + check);
                generateItems();
            } else {
                allowedLogs.remove(check);
                setCurrentPage(1);
                info.getMenu().setItem(info.getIndex(), info.getButton());
                generateItems();
            }

        }, () ->  new ItemBuilder(allowedLogs.contains(check)
                ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK).amount(1)
                .name(Color.Gold + check).build(), 5)));
        filterMenu.showMenu(player);
        filterMenu.setCloseHandler((pl, menu) -> Logs.this.showMenu(pl));
    }

    private void updateLogs(int limit, Runnable onUpdate) {
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, limit, logs -> {
            this.logs.clear();
            this.logs.addAll(logs);
            if(onUpdate != null)
                onUpdate.run();
        });
    }

    private void updateLogs(String check, int limit, Runnable onUpdate) {
        Anticheat.INSTANCE.getLogManager().getLogs(uuid, check, limit, 0, logs -> {
            this.logs.clear();
            this.logs.addAll(logs);
            if(onUpdate != null)
                onUpdate.run();
        });
    }

    private void generateItems() {
        contents.clear();
        Button button = new Button(false, new ItemBuilder(Material.REDSTONE).amount(1)
                .name(Color.Red + "Modify Filter").build(),
                (player, info) -> showFilterMenu(player));

        getFixedItems().put(getMenuDimension().getSize() - 5, button);
        for (Log log : logs) {
            if(!allowedLogs.isEmpty() && !allowedLogs.contains(log.getCheckId())) continue;

            ItemBuilder builder = new ItemBuilder(XMaterial.PAPER.parseMaterial()).amount(1)
                    .name(Color.Gold + log.getCheckName());

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
        buildInventory(getHolder() == null);
        generatedItems = true;
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
        }.runTaskTimer(Anticheat.INSTANCE, 0L, 4L);
    }


}
