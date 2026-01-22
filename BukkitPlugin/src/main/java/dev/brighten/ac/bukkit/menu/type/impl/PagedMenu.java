package dev.brighten.ac.bukkit.menu.type.impl;

import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.ItemBuilder;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.menu.Menu;
import dev.brighten.ac.utils.menu.button.Button;
import dev.brighten.ac.utils.menu.type.BukkitInventoryHolder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class PagedMenu implements Menu {
    @Getter
    @Setter
    String title;
    final MenuDimension dimension;
    @Setter
    private Menu parent;
    @Getter
    @Setter
    private int currentPage = 1;
    @Getter
    BukkitInventoryHolder holder;
    public final List<Button> contents = new ArrayList<>();
    @Getter
    private final Map<Integer, Button> fixedItems = new HashMap<>();
    private CloseHandler closeHandler;
    private final List<Consumer<Integer>> onPageChange = new ArrayList<>();

    public PagedMenu(@NonNull String title, int size) {
        this.title = title.length() > 32 ? title.substring(0, 32) : title;
        if (size <= 0 || size > 6) {
            throw new IndexOutOfBoundsException("A menu can only have between 1 & 6 for a size (rows)");
        }
        this.dimension = new MenuDimension(size, 9);
    }

    @Override
    public MenuDimension getMenuDimension() {
        return dimension;
    }

    @Override
    public void addItem(Button button) {
        contents.add(button);
    }

    @Override
    public void setItem(int index, Button button) {
        contents.set(index, button);
    }

    @Override
    public void fill(Button button) {
        fillRange(0, dimension.getSize(), button);
    }

    @Override
    public void fillRange(int startingIndex, int endingIndex, Button button) {
        IntStream.range(startingIndex, endingIndex)
                .filter(i -> contents.get(i) == null || BlockUtils.getXMaterial(contents.get(i).getStack().getType())
                        .equals(XMaterial.AIR))
                .forEach(i -> setItem(i, button));
    }

    @Override
    public int getFirstEmptySlot() {
        return contents.size();
    }

    @Override
    public void checkBounds(int index) {

    }

    public void onPageChange(Consumer<Integer> consumer) {
        onPageChange.add(consumer);
    }

    @Override
    public Optional<Button> getButtonByIndex(int index) {
        if(fixedItems.containsKey(index)) {
            return Optional.of(fixedItems.get(index));
        }
        if(index >= contents.size() - 1) return Optional.empty();

        return Optional.ofNullable(contents.get(index));
    }

    @Override
    public void buildInventory(boolean initial) {
        if (initial) {
            this.holder = new BukkitInventoryHolder(this);
            holder.setInventory(Bukkit.createInventory(holder, dimension.getSize(), title));
        }
        holder.getInventory().clear();

        val previous = new ItemBuilder(Material.BOOK).amount(currentPage - 1).name("&ePrevious Page").build();
        if(currentPage > 1) {
            fixedItems.put(dimension.getSize() - 6, new Button(false, previous,
                    (player, info) -> {
                        if(currentPage > 1) {
                            currentPage--;
                            onPageChange.forEach(consumer -> consumer.accept(currentPage));
                            buildInventory(false);
                        }
                    }));
        } else {
            fixedItems.put(dimension.getSize() - 6, new Button(false, new ItemBuilder(Material.BARRIER)
                    .name("&cNo Previous Page").build()));
        }
        val next = new ItemBuilder(Material.BOOK).amount(currentPage + 1).name("&eNext Page").build();

        int size = (dimension.getRows() - 1) * dimension.getColumns();

        if(contents.size() > size * currentPage) {
            fixedItems.put(dimension.getSize() - 4,  new Button(false, next, (player, info) -> {
                currentPage++;
                onPageChange.forEach(consumer -> consumer.accept(currentPage));
                buildInventory(false);
            }));
        } else {
            fixedItems.put(dimension.getSize() - 4, new Button(false, new ItemBuilder(Material.BARRIER)
                    .name("&cNo Next Page").build()));
        }
        AtomicInteger index = new AtomicInteger(0);
        IntStream.range(Math.min(contents.size(), size * (currentPage - 1)),
                        Math.min(contents.size(), size * currentPage))
                .forEach(i -> {
                    Button button = contents.get(i);
                    if (button != null) {
                        holder.getInventory().setItem(index.getAndIncrement(), button.getStack());
                    }
                });

        fixedItems.forEach((integer, button) -> holder.getInventory().setItem(integer, button.getStack()));
    }

    @Override
    public void showMenu(Player player) {
        buildInventory(holder == null);
        player.openInventory(holder.getInventory());
    }

    @Override
    public void close(Player player) {
        player.closeInventory();
        handleClose(player);
    }

    @Override
    public void setCloseHandler(CloseHandler handler) {
        this.closeHandler = handler;
    }

    @Override
    public void handleClose(Player player) {
        if (closeHandler != null) {
            closeHandler.accept(player, this);
        }
    }

    @Override
    public Optional<Menu> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Iterator<Button> iterator() {
        return contents.iterator();
    }
}
