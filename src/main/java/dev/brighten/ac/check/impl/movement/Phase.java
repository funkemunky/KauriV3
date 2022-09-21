package dev.brighten.ac.check.impl.movement;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

public class Phase extends Check {

    private static final Set<Material> allowedMaterials = EnumSet.noneOf(Material.class);

    static {
        Arrays.stream(Material.values())
                .filter(mat -> mat.name().contains("BANNER") || mat.name().contains("BREWING")
                        || mat.name().contains("CAULDRON") || mat.name().contains("PISTON"))
                .forEach(allowedMaterials::add);

        allowedMaterials.add(XMaterial.VINE.parseMaterial());
        allowedMaterials.add(XMaterial.CAKE.parseMaterial());
        if (ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)) {
            allowedMaterials.add(XMaterial.SCAFFOLDING.parseMaterial());
        }
    }

    public Phase(APlayer player) {
        super(player);
    }

    private final Timer lastFlag = new TickTimer(5);
    private KLocation fromWhereShitAintBad = null;


    WTimedAction<WPacketPlayInFlying> packet = (packet, now) -> {
        if(!packet.isMoved() || player.getCreation().isNotPassed(800L)
                || ((player.getInfo().lastRespawn.isNotPassed(500L)
                || player.getMovement().getMoveTicks() == 0) && lastFlag.isPassed(12))
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            return;
        }

        TagsBuilder tags = new TagsBuilder();

        SimpleCollisionBox
                toUpdate = new SimpleCollisionBox(player.getMovement().getTo().getLoc(), 0.6,1.8)
                .expand(-0.0825),
                playerBox = new SimpleCollisionBox(player.getBukkitPlayer().getLocation(), 0.6, 1.8)
                        .expand(-0.0825);

        SimpleCollisionBox concatted = Helper.wrap(playerBox, toUpdate);

        List<Block> blocks = Helper.blockCollisions(player.getBlockInfo().blocks, concatted);

        phaseIntoBlock: {
            List<Block> current = Helper.blockCollisions(blocks, playerBox),
                    newb = Helper.blockCollisions(blocks, toUpdate);

            for (Block block : newb) {
                if(!current.contains(block)) {
                    Material type = block.getType();
                    if(Materials.checkFlag(type, Materials.SOLID)
                            && !allowedMaterials.contains(type)
                            && !Materials.checkFlag(type, Materials.STAIRS)) {
                        tags.addTag("INTO_BLOCK");
                        tags.addTag("material=" + type.name());
                        vl++;
                        break;
                    } else debug(type.name());
                }
            }
        }

        phaseThru: {
            if(playerBox.isIntersected(toUpdate)) break phaseThru;

            Vector to = player.getMovement().getTo().getLoc().toVector(),
                    from =player.getMovement().getFrom().getLoc().toVector();

            to.add(new Vector(0, player.getInfo().sneaking ? 1.54f : 1.62f, 0));
            from.add(new Vector(0, player.getInfo().lsneaking ? 1.54f : 1.62f, 0));

            double dist = to.distance(from);

            Vector direction = to.subtract(from);
            RayCollision ray = new RayCollision(from, direction);

            for (Block block : blocks) {
                Material type = block.getType();
                if(!Materials.checkFlag(type, Materials.SOLID)
                        || allowedMaterials.contains(type) || Materials.checkFlag(type, Materials.STAIRS))
                    continue;

                CollisionBox box = BlockData.getData(type).getBox(block, player.getPlayerVersion());

                if(box instanceof SimpleCollisionBox) {
                    Tuple<Double, Double> result = new Tuple<>(0., 0.);
                    boolean intersected = RayCollision.intersect(ray, (SimpleCollisionBox) box);

                    if(intersected && result.one <= dist) {
                        vl++;
                        tags.addTag("THROUGH_BLOCK");
                        tags.addTag("material=" + type);
                        break;
                    }

                } else {
                    List<SimpleCollisionBox> downcasted = new ArrayList<>();

                    box.downCast(downcasted);

                    boolean flagged = false;
                    for (SimpleCollisionBox sbox : downcasted) {
                        Tuple<Double, Double> result = new Tuple<>(0., 0.);
                        boolean intersected = RayCollision.intersect(ray, sbox);

                        if(intersected && result.one <= dist) {
                            flagged = true;
                            break;
                        }
                    }

                    if(flagged) {
                        vl++;
                        tags.addTag("THROUGH_BLOCK");
                        tags.addTag("material=" + type);
                        break;
                    }
                }
            }
        }

        if(tags.getSize() > 0) {
            flag("tags=%s", tags.build());
            if(fromWhereShitAintBad == null) fromWhereShitAintBad = player.getMovement().getFrom().getLoc();
            final Location finalSetbackLocation = fromWhereShitAintBad.toLocation(player.getBukkitPlayer().getWorld());
            if(finalSetbackLocation != null) {
                RunUtils.task(() -> player.getBukkitPlayer().teleport(finalSetbackLocation));
            }
            lastFlag.reset();
        } else if(lastFlag.isPassed(5) && !player.getBlockInfo().collidesHorizontally) {
            fromWhereShitAintBad = player.getMovement().getFrom().getLoc().clone();
        }
    };
}
