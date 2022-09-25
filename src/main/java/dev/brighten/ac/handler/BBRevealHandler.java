package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.ItemBuilder;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Init
public class BBRevealHandler implements Listener {

    private final Set<Block> blocksToShow = new HashSet<>();
    private final Set<Entity> entitiesToShow = new HashSet<>();

    public static BBRevealHandler INSTANCE;

    private static final ItemStack wand = new ItemBuilder(Material.BLAZE_ROD).name("&6Box Wand")
            .amount(1).build();

    public BBRevealHandler() {
        INSTANCE = this;
        runShowTask();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(!event.getPlayer().getItemInHand().isSimilar(wand)) return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(blocksToShow.contains(event.getClickedBlock())) {
                blocksToShow.remove(event.getClickedBlock());
                event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing block.")
                        .color(ChatColor.RED).create());
            } else {
                blocksToShow.add(event.getClickedBlock());
                event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing block.")
                        .color(ChatColor.GREEN).create());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if(!event.getPlayer().getItemInHand().isSimilar(wand)) return;

        if(entitiesToShow.contains(event.getRightClicked())) {
            entitiesToShow.remove(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.RED).create());
        } else {
            entitiesToShow.add(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.GREEN).create());
        }
    }

    public void giveWand(Player player) {
        player.getInventory().addItem(wand);
    }

    private void runShowTask() {
        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            blocksToShow.forEach(b -> BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion())
                    .draw(EnumParticle.FLAME, Bukkit.getOnlinePlayers().toArray(new Player[0])));
            entitiesToShow.forEach(e -> {
                SimpleCollisionBox box;
                if((box =Helper.getEntityCollision(e)) != null) {
                    box.draw(EnumParticle.FLAME, Bukkit.getOnlinePlayers().toArray(new Player[0]));
                } else {
                    EntityData.getEntityBox(e.getLocation(), e)
                            .draw(EnumParticle.FLAME, Bukkit.getOnlinePlayers().toArray(new Player[0]));
                }
            });
        }, 3000, 500, TimeUnit.MILLISECONDS);
    }

}
