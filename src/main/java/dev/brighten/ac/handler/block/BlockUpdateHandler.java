package dev.brighten.ac.handler.block;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockDig;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutBlockChange;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutMultiBlockChange;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.wrapper.Wrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

@RequiredArgsConstructor
public class BlockUpdateHandler {
    private final Map<IntVector, Deque<Material>> blockInformation = new Object2ObjectOpenHashMap<>();

    private final APlayer player;

    public void onWorldChange() {
        blockInformation.clear();
    }

    /**
     * Keep track of block placements since the Bukkit API will be a bit behind
     * @param place
     */
    public void onPlace(WPacketPlayInBlockPlace place) {
        player.getInfo().lastBlockUpdate.reset();
        // Could not possibly be a block placement as it's not a block a player is holding.
        IntVector pos = place.getBlockPos().clone();

        // Some dumbass shit I have to do because Minecraft with Lilypads
        if(place.getItemStack() != null && BlockUtils.getXMaterial(place.getItemStack().getType()).equals(XMaterial.LILY_PAD)) {
            RayCollision rayCollision = new RayCollision(player.getBukkitPlayer().getEyeLocation().toVector(),
                    player.getBukkitPlayer().getLocation().getDirection());
            Block block = rayCollision.getClosestBlockOfType(player.getBukkitPlayer().getWorld(), Materials.LIQUID, 5);

            if(block != null) {
                if (Materials.checkFlag(block.getType(), Materials.WATER)) {
                    pos = new IntVector(block.getX(), block.getY() + 1, block.getZ());
                }
            } else return;
        } // Not an actual block place, just an interact
        else if(pos.getX() == -1 && (pos.getY() == 255 || pos.getY() == -1) && pos.getZ() == -1) {
            return;
        } else {
            pos.setX(pos.getX() + place.getDirection().getAdjacentX());
            pos.setY(pos.getY() + place.getDirection().getAdjacentY());
            pos.setZ(pos.getZ() + place.getDirection().getAdjacentZ());
        }

        player.getInfo().getLastPlace().reset();

        Deque<Material> possible = getPossibleMaterials(pos);
        possible.add(place.getItemStack().getType());
    }

    /**
     * Keep track of block breaking since the Bukkit API will be a bit behind.
     * @param dig
     */
    public void onDig(WPacketPlayInBlockDig dig) {
        player.getInfo().lastBlockUpdate.reset();
        if(dig.getDigType() == WPacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
            Deque<Material> possible = getPossibleMaterials(dig.getBlockPos());
            possible.clear();
            possible.add(Material.AIR);
        }
    }

    public void runUpdate(WPacketPlayOutBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();
        synchronized (blockInformation) {
            Deque<Material> blockInfo = blockInformation.compute(packet.getBlockLocation(), (blockLoc, blockI) -> {
                if(blockI == null) {
                    blockI = new LinkedList<>();

                    val optional = BlockUtils
                            .getBlockAsync(packet.getBlockLocation().toBukkitVector()
                                    .toLocation(player.getBukkitPlayer().getWorld()));

                    if(optional.isPresent()) {
                        Block block = optional.get();

                        blockI.add(block.getType());
                    }
                }

                return blockI;
            });
            // Updating block information
            player.runInstantAction(k -> {
                if (!k.isEnd()) {
                    blockInfo.add(packet.getMaterial());
                } else if (blockInfo.size() > 1) {
                    blockInfo.removeFirst();
                }
            });
        }
    }

    public void runUpdate(WPacketPlayOutMultiBlockChange packet) {
        player.getInfo().lastBlockUpdate.reset();
        List<Tuple<Deque<Material>, Material>> changes = new ArrayList<>();
        synchronized (blockInformation) {
            for (WPacketPlayOutMultiBlockChange.BlockChange change : packet.getChanges()) {
                Deque<Material> blockInfo = blockInformation.compute(change.getLocation(), (blockLoc, blockI) -> {
                    if(blockI == null) {
                        blockI = new LinkedList<>();

                        val optional = BlockUtils
                                .getBlockAsync(change.getLocation().toBukkitVector()
                                        .toLocation(player.getBukkitPlayer().getWorld()));

                        if(optional.isPresent()) {
                            Block block = optional.get();

                            blockI.add(block.getType());
                        }
                    }

                    return blockI;
                });

                changes.add(new Tuple<>(blockInfo, change.getMaterial()));
            }

        }
        player.runInstantAction(k -> {
            if(!k.isEnd()) {
                for (Tuple<Deque<Material>, Material> tuple : changes) {
                    tuple.one.add(tuple.two);
                }
            } else {
                for (Tuple<Deque<Material>, Material> tuple : changes) {
                    if(tuple.one.size() > 1) {
                        tuple.one.removeFirst();
                    }
                }
            }
        });
    }

    public Deque<Material> getPossibleMaterials(IntVector loc) {
        synchronized (blockInformation) {

            Deque<Material> blockI = blockInformation.get(loc);

            if(blockI == null) {
                blockI = new LinkedList<>();

                Material type = Wrapper.getInstance().getType(player.getBukkitPlayer().getWorld(),
                        loc.getX(), loc.getY(), loc.getZ());

                blockI.add(type);

                blockInformation.put(loc, blockI);
            }

            return blockI;
        }
    }
}
