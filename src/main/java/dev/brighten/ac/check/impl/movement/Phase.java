package dev.brighten.ac.check.impl.movement;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@CheckData(name = "Phase", checkId = "phase", type = CheckType.MOVEMENT)
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

    private int ticks;

    private KLocation toSetback = null;


    WCancellable<WPacketPlayInFlying> packet = (packet) -> {
        if(packet.isMoved() && ticks < 3) {
            ticks++;
            return false;
        }

        if(toSetback != null && packet.isMoved()) {
            if(player.getMovement().getTo().getLoc().toVector().distanceSquared(toSetback.toVector()) < 0.0001) {
                toSetback = null;
                debug("Reached loc");
            } else {
                RunUtils.task(() -> player.getBukkitPlayer().teleport(toSetback
                        .toLocation(player.getBukkitPlayer().getWorld())));
                debug("Hasnt reached location");
                return true;
            }
        }
        if(!packet.isMoved() || player.getCreation().isNotPassed(800L)
                || player.getInfo().lastRespawn.isNotPassed(10)
                || (player.getMovement().getMoveTicks() == 0
                && player.getMovement().getPosLocs().stream()
                .anyMatch(kloc -> kloc.toVector()
                        .distanceSquared(player.getMovement().getTo().getLoc().toVector()) < 0.01))
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            debug("Returned: " + player.getMovement().getMoveTicks() + "," + player.getMovement().getPosLocs().stream()
                    .anyMatch(kloc -> kloc.toVector()
                            .distanceSquared(player.getMovement().getTo().getLoc().toVector()) < 0.01));
            return false;
        }

        SimpleCollisionBox fromBox = player.getMovement().getFrom().getBox().copy(), toBox = fromBox.copy();

        double deltaX = player.getMovement().getDeltaX(), deltaY = player.getMovement().getDeltaY(),
                deltaZ = player.getMovement().getDeltaZ();

        List<SimpleCollisionBox> collisions = Helper.getCollisions(player, fromBox.copy().addCoord(deltaX, deltaY, deltaZ),
                Materials.SOLID);

        for (SimpleCollisionBox collision : collisions) {
            deltaY = collision.calculateYOffset(toBox, deltaY);
        }

        toBox.offset(0, deltaY, 0);

        for (SimpleCollisionBox collision : collisions) {
            deltaX = collision.calculateXOffset(toBox, deltaX);
        }

        toBox.offset(deltaX, 0, 0);

        for (SimpleCollisionBox collision : collisions) {
            deltaZ = collision.calculateZOffset(toBox, deltaZ);
        }

        toBox.offset(0, 0, deltaZ);

        KLocation calculatedTo = player.getMovement().getFrom().getLoc().clone().add(deltaX, deltaY, deltaZ);

        double dx = Math.abs(deltaX - player.getMovement().getDeltaX()),
                dy = Math.abs(deltaY - player.getMovement().getDeltaY()),
                dz = Math.abs(deltaZ - player.getMovement().getDeltaZ());

        double totalDelta = dx + dy + dz;

        if(totalDelta > 0.001) {
            RunUtils.task(() -> player.getBukkitPlayer().teleport(calculatedTo
                    .toLocation(player.getBukkitPlayer().getWorld())));
            flag("x=%.4f, y=%.4f, z=%.4f", dx, dy, dz);
        }

        debug("(%s) [%.5f]: new=[%.3f, %.3f, %.3f] old=[%.3f, %.3f, %.3f]", collisions.size(), totalDelta, deltaX, deltaY, deltaZ,
                player.getMovement().getDeltaX(), player.getMovement().getDeltaY(),
                player.getMovement().getDeltaZ());
        return false;
    };
}
