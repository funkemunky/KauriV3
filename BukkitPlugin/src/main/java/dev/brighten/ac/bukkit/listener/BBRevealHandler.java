package dev.brighten.ac.bukkit.listener;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.bukkit.utils.ItemBuilder;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.annotation.Init;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Init
public class BBRevealHandler implements Listener {

    private final Map<UUID, Set<Vector3i>> blocksToShow = new HashMap<>();
    private final Set<Entity> entitiesToShow = new HashSet<>();

    public static BBRevealHandler INSTANCE;

    private final ItemStack wand = new ItemBuilder(Material.BLAZE_ROD).name("&6Box Wand")
            .amount(1).build();

    public BBRevealHandler() {
        INSTANCE = this;
        runShowTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry()
                .getPlayer(event.getPlayer().getUniqueId()).orElse(null);

        if(player == null || !player.getBukkitPlayer().getInventory().getItemInHand().isSimilar(wand)) return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Vector3i blockLoc = new Vector3i(event.getClickedBlock().getX(),
                    event.getClickedBlock().getY(), event.getClickedBlock().getZ());
            Set<Vector3i> blocksToShow = this.blocksToShow
                    .computeIfAbsent(event.getPlayer().getUniqueId(), k -> new HashSet<>());
            if(blocksToShow.contains(blockLoc)) {
                blocksToShow.remove(blockLoc);
                event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing block: ")
                        .color(ChatColor.RED).append(event.getClickedBlock().getType().name()).color(ChatColor.WHITE)
                        .create());
            } else {
                blocksToShow.add(blockLoc);
                WrappedBlock block = player.getWorldTracker().getBlock(blockLoc);
                event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing block: ")
                        .color(ChatColor.GREEN).color(ChatColor.WHITE).append(event.getClickedBlock().getType().name())
                        .color(ChatColor.GRAY)
                                .append(" flags: " + block.getBlockState().getType() + " | " + Materials.checkFlag(block.getType(), Materials.COLLIDABLE) + "," +
                                        Materials.checkFlag(block.getType(), Materials.SOLID) + "," + Materials.checkFlag(block.getType(), Materials.LIQUID))
                                .color(ChatColor.RED)
                                .append(" | box=" + BlockData.getData(block.getType()).getBox(player, blockLoc, player.getPlayerVersion()).downCast().stream().map(SimpleCollisionBox::toString).collect(Collectors.joining(", ")))
                        .create());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(!event.getPlayer().getItemInHand().isSimilar(wand)) return;

        if(entitiesToShow.contains(event.getRightClicked())) {
            entitiesToShow.remove(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.RED).create());
            event.setCancelled(true);
        } else {
            entitiesToShow.add(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.GREEN).create());
            event.setCancelled(true);
        }
    }

    public void giveWand(Player player) {
        player.getInventory().addItem(wand);
    }

    private void runShowTask() {
        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            blocksToShow.forEach((uuid, blocks) -> {
                Optional<APlayer> player = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(uuid);
                if(player.isEmpty()) return;

                blocks.forEach(blockLoc -> {
                    var block = player.get().getWorldTracker().getBlock(blockLoc);

                    var blockBox = BlockData.getData(block.getType())
                            .getBox(player.get(), blockLoc, player.get().getPlayerVersion());

                    blockBox.draw(ParticleTypes.FLAME, player.get());
                });
            });
            entitiesToShow.forEach(e -> EntityData.getEntityBox(e)
                    .draw(ParticleTypes.FLAME, Anticheat.INSTANCE.getPlayerRegistry().aplayerMap.values()
                            .toArray(APlayer[]::new)));
        }, 3000, 250, TimeUnit.MILLISECONDS);
    }

}
