package dev.brighten.ac.check.impl.movement;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutPosition;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CheckData(name = "Phase", checkId = "phase", type = CheckType.MOVEMENT)
public class Phase extends Check {

    public Phase(APlayer player) {
        super(player);
    }

    private int ticks;
    private KLocation toSetback = null;

    private Set<Vector> positions = new HashSet<>();

    WAction<WPacketPlayOutPosition> positionOut = packet -> {
        KLocation loc = new KLocation(packet.getX(), packet.getY(), packet.getZ(),
                packet.getYaw(), packet.getPitch());
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
            loc.x += player.getMovement().getTo().getLoc().x;
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
            loc.y += player.getMovement().getTo().getLoc().y;
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
            loc.z += player.getMovement().getTo().getLoc().z;
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
            loc.pitch += player.getMovement().getTo().getLoc().pitch;
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
            loc.yaw += player.getMovement().getTo().getLoc().yaw;
        }

        positions.add(loc.toVector());
    };

    WCancellable<WPacketPlayInFlying> packet = (packet) -> {
        if(packet.isMoved() && ticks < 3) {
            ticks++;
            return false;
        }

        if(toSetback != null && packet.isMoved()) {
            if(player.getMovement().getTo().getLoc().distanceSquared(toSetback) < 1E-8) {
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
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            return false;
        }

        if(positions.size() > 0 && !packet.isOnGround() && positions.stream()
                .anyMatch(v -> v.distanceSquared(player.getMovement().getTo().getLoc().toVector()) < 0.0001)) {
            debug("Returned: [%s, %s, %s]", player.getMovement().getTo().getX(),
                    player.getMovement().getTo().getY(), player.getMovement().getTo().getZ());
            debug("Locs:");
            positions.stream().forEach(v -> debug("loc: [%s, %s, %s]", v.getX(), v.getY(), v.getZ()));
            return false;
        } else if(player.getMovement().getMoveTicks() > 0 && player.getMovement().getTeleportsToConfirm() == 0) {
            positions.clear();
        }

        if(player.getMovement().getFrom().getLoc().distanceSquared(player.getMovement().getTo().getLoc()) > 400) {
            MiscUtils.printToConsole(player.getBukkitPlayer().getName() + " moved too fast!");
            RunUtils.task(() -> player.getBukkitPlayer().teleport(player.getMovement().getFrom().getLoc()
                    .toLocation(player.getBukkitPlayer().getWorld())));
            return true;
        }

        SimpleCollisionBox fromBox = player.getMovement().getFrom().getBox().copy(), toBox = fromBox.copy();

        double deltaX = player.getMovement().getDeltaX(), deltaY = player.getMovement().getDeltaY(),
                deltaZ = player.getMovement().getDeltaZ();

        List<SimpleCollisionBox> collisions = Helper.getCollisionsNoEntities(player,
                fromBox.copy().addCoord(deltaX, deltaY, deltaZ), Materials.SOLID);

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
            // Fixing calculated teleport location going wonky. If its 0.05 away, just set it to the from lOC.
            if(calculatedTo.distanceSquared(player.getMovement().getFrom().getLoc()) > 0.0025) {
                calculatedTo.setLocation(player.getMovement().getFrom().getLoc());
            }

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
