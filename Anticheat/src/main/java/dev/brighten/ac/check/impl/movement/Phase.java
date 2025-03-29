package dev.brighten.ac.check.impl.movement;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutPosition;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingSet;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

@CheckData(name = "Phase", checkId = "phase", type = CheckType.MOVEMENT)
public class Phase extends Check {

    public Phase(APlayer player) {
        super(player);
    }

    private int ticks;
    private final Set<Vector> POSITIONS = new EvictingSet<>(10);
    private Location teleportLoc = null;

    @Bind
    WAction<WPacketPlayOutPosition> positionOut = packet -> {
        KLocation loc = new KLocation(packet.getX(), packet.getY(), packet.getZ(),
                packet.getYaw(), packet.getPitch());
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
            loc.add(player.getMovement().getTo().getLoc().getX(), 0, 0);
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
            loc.add(0, player.getMovement().getTo().getLoc().getY(), 0);
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
            loc.add(0, 0, player.getMovement().getTo().getLoc().getZ());
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
            loc.setPitch(loc.getPitch() + player.getMovement().getTo().getLoc().getPitch());
        }
        if (packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
            loc.setYaw(loc.getYaw() + player.getMovement().getTo().getLoc().getYaw());
        }

        POSITIONS.add(loc.toVector());
    };

    @Bind
    WCancellable<WPacketPlayInFlying> packet = (packet) -> {
        if(packet.isMoved() && ticks < 3) {
            ticks++;
            return false;
        }

        if(!packet.isMoved() || player.getCreation().isNotPassed(800L)
                || player.getInfo().lastRespawn.isNotPassed(10)
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            return false;
        }

        if(POSITIONS.size() > 0 && !packet.isOnGround() && POSITIONS.stream()
                .anyMatch(v -> v.distanceSquared(player.getMovement().getTo().getLoc().toVector()) < 0.0001)) {
            debug("Returned: [%s, %s, %s]", player.getMovement().getTo().getX(),
                    player.getMovement().getTo().getY(), player.getMovement().getTo().getZ());
            teleportLoc = null;
            return false;
        } else if(player.getMovement().getMoveTicks() > 0 && player.getMovement().getTeleportsToConfirm() == 0) {
            POSITIONS.clear();
        } else if(teleportLoc != null) {
            final Location finalLoc = teleportLoc.clone(); // This is to make sure it isn't set null later.
            Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(finalLoc));
            return true;
        }

        if(player.getMovement().getFrom().getLoc().distanceSquared(player.getMovement().getTo().getLoc()) > 400) {
            MiscUtils.printToConsole(player.getBukkitPlayer().getName() + " moved too fast!");
            // This is to make sure it isn't set null later.
            final Location fromLoc = player.getMovement().getFrom().getLoc()
                    .toLocation(player.getBukkitPlayer().getWorld());

            Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(fromLoc));
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

        KLocation calculatedTo = player.getMovement().getFrom().getLoc().clone();

        double dx = Math.abs(deltaX - player.getMovement().getDeltaX()),
                dy = Math.abs(deltaY - player.getMovement().getDeltaY()),
                dz = Math.abs(deltaZ - player.getMovement().getDeltaZ());

        double totalDelta = dx + dy + dz;

        if(totalDelta > 0.0001) {
            Anticheat.INSTANCE.getRunUtils().task(() -> {
                teleportLoc = calculatedTo
                        .toLocation(player.getBukkitPlayer().getWorld());
                player.getBukkitPlayer().teleport(teleportLoc);
            });
            flag("x=%.4f, y=%.4f, z=%.4f", dx, dy, dz);
        }

        debug("(%s) [%.5f]: new=[%.3f, %.3f, %.3f] old=[%.3f, %.3f, %.3f]", collisions.size(), totalDelta,
                deltaX, deltaY, deltaZ, player.getMovement().getDeltaX(), player.getMovement().getDeltaY(),
                player.getMovement().getDeltaZ());
        return false;
    };
}
