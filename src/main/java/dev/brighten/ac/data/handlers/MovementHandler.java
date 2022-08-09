package dev.brighten.ac.data.handlers;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.CMove;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.util.LinkedList;

@RequiredArgsConstructor
public class MovementHandler {

    private final APlayer player;

    @Getter
    private final CMove to = new CMove(), from = new CMove();
    @Getter
    private double deltaX, deltaY, deltaZ, deltaXZ,
            lDeltaX, lDeltaY, lDeltaZ, lDeltaXZ;
    @Getter
    private float deltaYaw, deltaPitch, lDeltaYaw, lDeltaPitch;
    private int moveTicks;

    private int teleportsToConfirm;

    private LinkedList<Float> yawGcdList = new EvictingList<>(45),
            pitchGcdList = new EvictingList<>(45);


    public void process(WPacketPlayInFlying packet, long currentTime) {
        player.getPotionHandler().onFlying(packet);
        if(moveTicks > 0) {
            updateLocations(packet);

            // Updating block locations
            player.getInfo().setBlockOnTo(BlockUtils
                    .getBlockAsync(to.getLoc().toLocation(player.getBukkitPlayer().getWorld())));
            player.getInfo().setBlockBelow(BlockUtils
                    .getBlockAsync(to.getLoc().toLocation(player.getBukkitPlayer().getWorld())
                            .subtract(0,1,0)));

            if(packet.isMoved()) {
                // Updating player bounding box
                player.getInfo().getLastMove().reset();
            }

            player.getBlockInformation().runCollisionCheck();
        }


        moveTicks++; //Must be end of code
    }

    /**
     * Updating the "to" and "from" location to current location.
     * Resetting position tracking; meant primarily for instant teleports.
     * @param location Location
     */
    public void moveTo(Location location) {
        to.getLoc().x = from.getLoc().x = location.getX();
        to.getLoc().y = from.getLoc().y = location.getY();
        to.getLoc().z = from.getLoc().z = location.getZ();
        to.getLoc().yaw = from.getLoc().yaw = location.getYaw();
        to.getLoc().pitch = from.getLoc().pitch = location.getPitch();

        deltaX = deltaY = deltaZ = deltaXZ
                = lDeltaX = lDeltaY = lDeltaZ
                = lDeltaXZ = 0;

        deltaYaw = lDeltaYaw =
                deltaPitch = lDeltaPitch = 0;
        moveTicks = 0;
        //doingTeleport = inventoryOpen  = false;
    }

    /**
     * Setting the to and from to current location only if the player either moved or looked.
     * @param packet WPacketPlayInFlyingh
     */
    private void setTo(WPacketPlayInFlying packet) {
        to.setWorld(player.getBukkitPlayer().getWorld());
        if(packet.isMoved()) {
            to.getLoc().x = packet.getX();
            to.getLoc().y = packet.getY();
            to.getLoc().z = packet.getZ();
        }
        if(packet.isLooked()) {
            to.getLoc().yaw = packet.getYaw();
            to.getLoc().pitch = packet.getPitch();
        }
        to.setBox(new SimpleCollisionBox(to.getLoc(), 0.6, 1.8));
        to.setOnGround(packet.isOnGround());
    }

    /**
     * If from location is null, update to loc after to is set, otherwise, update to before from.
     * Updates the location of player and its general delta movement.
     * @param packet WPacketPlayInFlying
     */
    private void updateLocations(WPacketPlayInFlying packet) {
        from.setLoc(to);
        setTo(packet);

        lDeltaX = deltaX;
        lDeltaY = deltaY;
        lDeltaZ = deltaZ;
        lDeltaXZ = deltaXZ;
        lDeltaYaw = deltaYaw;
        lDeltaPitch = deltaPitch;

        deltaX = to.getLoc().x - from.getLoc().x;
        deltaY = to.getLoc().y - from.getLoc().y;
        deltaZ = to.getLoc().z - from.getLoc().z;
        deltaXZ = Math.hypot(deltaX, deltaZ); // Calculating here to cache since hypot() can be heavy.
        deltaYaw = MathUtils.getAngleDelta(to.getLoc().yaw, from.getLoc().yaw);
        deltaPitch = to.getLoc().pitch - from.getLoc().pitch;
    }
}
