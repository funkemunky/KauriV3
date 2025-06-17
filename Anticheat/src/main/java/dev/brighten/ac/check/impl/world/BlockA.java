package dev.brighten.ac.check.impl.world;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.math.cond.MaxDouble;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@CheckData(name = "Block (A)", checkId = "blocka", type = CheckType.INTERACT)
public class BlockA extends Check {
    public BlockA(APlayer player) {
        super(player);
    }

    private final MaxDouble VERBOSE = new MaxDouble(20);
    private final Queue<Tuple<Block, SimpleCollisionBox>> PLACEMENTS = new LinkedBlockingQueue<>();

    @Bind
    WAction<WPacketPlayInBlockPlace> blockPlace = packet -> {
        Location loc = packet.getBlockPos().toBukkitVector().toLocation(player.getBukkitPlayer().getWorld());
        Optional<Block> optionalBlock = BlockUtils.getBlockAsync(loc);

        if(optionalBlock.isEmpty()) return;

        final Block block = optionalBlock.get();
        CollisionBox box = BlockData.getData(block.getType()).getBox(block, player.getPlayerVersion());

        debug(packet.getBlockPos().toString());
        if(!(box instanceof final SimpleCollisionBox simpleBox)) {
            debug("Not SimpleCollisionBox: " + box.getClass().getSimpleName() + ";" + block.getType());
            return;
        }

        if(Math.abs(simpleBox.maxY - simpleBox.minY) != 1.
                || Math.abs(simpleBox.maxX - simpleBox.minX) != 1.
                || Math.abs(simpleBox.maxZ - simpleBox.minZ) != 1.) {
            debug("not full block: x=%.1f y=%.1f z=%.1f",
                    Math.abs(simpleBox.maxX - simpleBox.minX),
                    Math.abs(simpleBox.maxY - simpleBox.minY),
                    Math.abs(simpleBox.maxZ - simpleBox.minZ));
            return;
        }

        PLACEMENTS.add(new Tuple<>(block, simpleBox.expand(0.1)));
    };

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        Tuple<Block, SimpleCollisionBox> tuple;

        while((tuple = PLACEMENTS.poll()) != null) {
            final SimpleCollisionBox box = tuple.two.copy().expand(0.025);
            final Block block = tuple.one;

            final KLocation to = player.getMovement().getTo().getLoc().clone().add(0, player.getEyeHeight(), 0),
                    from = player.getMovement().getFrom().getLoc().clone().add(0, player.getPreviousEyeHeight(), 0);

            final RayCollision rayTo = new RayCollision(to.toVector(),
                    MathUtils.getDirection(to)),
                    rayFrom = new RayCollision(from.toVector(),
                            MathUtils.getDirection(from));

            final boolean collided = rayTo.isCollided(box) || rayFrom.isCollided(box);

            if (!collided) {
                if (VERBOSE.add() > 4) {
                    flag("to=[x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f] loc=[%.1f,%.1f,%.1f]",
                            to.getX(), to.getY(), to.getZ(), to.getYaw(), from.getPitch(),
                            block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
                }
            } else VERBOSE.subtract(0.33);

            debug("collided=%s verbose=%s", collided, VERBOSE.value());
        }
    };
}
