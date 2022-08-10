package dev.brighten.ac.data.handlers;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.CMove;
import dev.brighten.ac.handler.compat.CompatHandler;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutPosition;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    private final List<KLocation> posLocs = new ArrayList<>();
    @Getter
    private boolean checkMovement;
    @Getter
    @Setter
    private boolean excuseNextFlying;

    @Getter
    private final Timer lastTeleport = new TickTimer();

    private int teleportsToConfirm;

    private final LinkedList<Float> yawGcdList = new EvictingList<>(45),
            pitchGcdList = new EvictingList<>(45);


    public void process(WPacketPlayInFlying packet, long currentTime) {

        player.getPotionHandler().onFlying(packet);

        checkMovement = MovementUtils.checkMovement(player.getPlayerConnection());

        if(checkMovement) {
            moveTicks++;
        }
        else moveTicks = 0;

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

        checkForTeleports(packet);

        player.getInfo().setCreative(player.getBukkitPlayer().getGameMode() == GameMode.CREATIVE
                || player.getBukkitPlayer().getGameMode() == GameMode.SPECTATOR);

        boolean hasLevitation = ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)
                && player.getPotionHandler().hasPotionEffect(XPotion.LEVITATION.getPotionEffectType());

        player.getInfo().setGeneralCancel(player.getBukkitPlayer().getAllowFlight()
                || moveTicks == 0
                || excuseNextFlying
                || player.getInfo().isCreative()
                || lastTeleport.isNotPassed(2)
                || teleportsToConfirm > 0
                || player.getInfo().isInVehicle()
                || player.getInfo().getVehicleSwitch().isNotPassed(1)
                || player.getBukkitPlayer().isSleeping()
                || CompatHandler.getInstance().isRiptiding(player.getBukkitPlayer())
                || CompatHandler.getInstance().isGliding(player.getBukkitPlayer())
                || hasLevitation);

        /*
        ata.playerInfo.generalCancel = data.getPlayer().getAllowFlight()
                || data.playerInfo.creative
                || hasLevi
                || data.excuseNextFlying
                || data.getPlayer().isSleeping()
                || (data.playerInfo.lastGhostCollision.isNotPassed() && data.playerInfo.lastBlockPlace.isPassed(2))
                || data.playerInfo.doingTeleport
                || data.playerInfo.lastTeleportTimer.isNotPassed(1)
                || data.playerInfo.riptiding
                || data.playerInfo.gliding
                || data.playerInfo.vehicleTimer.isNotPassed(3)
                || data.playerInfo.lastPlaceLiquid.isNotPassed(5)
                || data.playerInfo.inVehicle
                || ((data.playerInfo.lastChunkUnloaded.isNotPassed(35) || data.playerInfo.doingBlockUpdate)
                && MathUtils.getDelta(-0.098, data.playerInfo.deltaY) < 0.0001)
                || timeStamp - data.playerInfo.lastRespawn < 2500L
                || data.playerInfo.lastToggleFlight.isNotPassed(40)
                || timeStamp - data.creation < 4000
                || Kauri.INSTANCE.lastTickLag.isNotPassed(5);
         */
    }

    public void addPosition(WPacketPlayOutPosition packet) {
        int i = 0;
        KLocation loc = new KLocation(packet.getX(), packet.getY(), packet.getZ(),
                packet.getYaw(), packet.getPitch());
        if(packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
            loc.x+= player.getMovement().getTo().getLoc().x;
        }
        if(packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
            loc.y+= player.getMovement().getTo().getLoc().y;
        }
        if(packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
            loc.z+= player.getMovement().getTo().getLoc().z;
        }
        if(packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
            loc.pitch+= player.getMovement().getTo().getLoc().pitch;
        }
        if(packet.getFlags().contains(WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
            loc.yaw+= player.getMovement().getTo().getLoc().yaw;
        }

        teleportsToConfirm++;

        player.runKeepaliveAction(ka -> teleportsToConfirm--, 2);
        synchronized (posLocs) {
            posLocs.add(loc);
        }
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

    private void checkForTeleports(WPacketPlayInFlying packet) {
        if(packet.isMoved() && packet.isLooked() && !packet.isOnGround()) {
            synchronized (posLocs) {
                Iterator<KLocation> iterator = posLocs.iterator();

                //Iterating through the ArrayList to find a potential teleport. We can't remove from the list
                //without causing a CME unless we use Iterator#remove().
                while(iterator.hasNext()) {
                    KLocation posLoc = iterator.next();

                    KLocation to = new KLocation(packet.getX(), packet.getY(), packet.getZ());
                    double distance = MathUtils.getDistanceWithoutRoot(to, posLoc);

                    if(distance < 1E-9) {
                        lastTeleport.reset();
                        iterator.remove();
                        break;
                    }
                }

                //Ensuring the list doesn't overflow with old locations, a potential crash exploit.
                if(teleportsToConfirm == 0 && posLocs.size() > 0) {
                    posLocs.clear();
                }
            }
        }
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
