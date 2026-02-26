package dev.brighten.ac.check.impl.movement;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingSet;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;

@CheckData(name = "Phase", checkId = "phase", type = CheckType.MOVEMENT)
public class Phase extends Check {

    public Phase(APlayer player) {
        super(player);
    }

    private int ticks;
    private final Set<Vector3d> POSITIONS = new EvictingSet<>(10);
    private Location teleportLoc = null;

    @Bind
    WAction<WrapperPlayServerPlayerPositionAndLook> positionOut = packet -> {
        KLocation loc = new KLocation(packet.getX(), packet.getY(), packet.getZ(),
                packet.getYaw(), packet.getPitch());

        RelativeFlag flags = packet.getRelativeFlags();

        if (flags.has(RelativeFlag.X)) {
            loc.add(packet.getX(), 0, 0);
        }

        if (flags.has(RelativeFlag.Y)) {
            loc.add(0, packet.getY(), 0);
        }

        if (flags.has(RelativeFlag.Z)) {
            loc.add(0, 0, packet.getZ());
        }

        if (flags.has(RelativeFlag.YAW)) {
            loc.setPitch(loc.getPitch() + packet.getYaw());
        }

        if (flags.has(RelativeFlag.PITCH)) {
            loc.setYaw(loc.getYaw() + packet.getPitch());
        }

        POSITIONS.add(loc.toVector());
    };

    @Bind
    WCancellable<WrapperPlayClientPlayerFlying> packet = (packet) -> {
        if(packet.hasPositionChanged() && ticks < 3) {
            ticks++;
            return false;
        }

        if(!packet.hasPositionChanged() || player.getCreation().isNotPassed(800L)
                || player.getInfo().lastRespawn.isNotPassed(10)
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            return false;
        }

        if(!POSITIONS.isEmpty() && !packet.isOnGround() && POSITIONS.stream()
                .anyMatch(v -> v.distanceSquared(player.getMovement().getTo().getLoc().toVector()) < 0.0001)) {
            debug("Returned: [%s, %s, %s]", player.getMovement().getTo().getX(),
                    player.getMovement().getTo().getY(), player.getMovement().getTo().getZ());
            teleportLoc = null;
            return false;
        } else if(player.getMovement().getMoveTicks() > 0 && player.getMovement().getTeleportsToConfirm() == 0) {
            POSITIONS.clear();
        } else if(teleportLoc != null) {
            final Location finalLoc = teleportLoc.clone(); // This is to make sure it isn't set null later.
            if(isCancellable()) {
                Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(finalLoc));
            }
            return true;
        }

        if(player.getMovement().getFrom().getLoc().distanceSquared(player.getMovement().getTo().getLoc()) > 400) {
            MiscUtils.printToConsole(player.getBukkitPlayer().getName() + " moved too fast!");
            // This is to make sure it isn't set null later.
            final Location fromLoc = player.getMovement().getFrom().getLoc()
                    .toLocation(player.getBukkitPlayer().getWorld());

            if(isCancellable()) {
                Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(fromLoc));
            }
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

        if(totalDelta > 0.0001 && (player.getPlayerVersion().isOlderThan(ClientVersion.V_1_21_5) || dy > 0.003 || (dx + dz) > 0.001)) {
            if(isCancellable()) {
                Anticheat.INSTANCE.getRunUtils().task(() -> {
                    teleportLoc = calculatedTo
                            .toLocation(player.getBukkitPlayer().getWorld());
                    player.getBukkitPlayer().teleport(teleportLoc);
                });
            }
            flag("x=%.4f, y=%.4f, z=%.4f", dx, dy, dz);
        }

        debug("(%s) [%.5f]: new=[%.3f, %.3f, %.3f] old=[%.3f, %.3f, %.3f]", collisions.size(), totalDelta,
                deltaX, deltaY, deltaZ, player.getMovement().getDeltaX(), player.getMovement().getDeltaY(),
                player.getMovement().getDeltaZ());
        return false;
    };
}
