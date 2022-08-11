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
import lombok.val;
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
    private float lookX, lookY, lastLookX, lastLookY;
    @Getter
    private float deltaYaw, deltaPitch, lDeltaYaw, lDeltaPitch, smoothYaw, smoothPitch, lsmoothYaw, lsmoothPitch, pitchGCD, lastPitchGCD, yawGCD, lastYawGCD;
    private int moveTicks;
    private final List<KLocation> posLocs = new ArrayList<>();
    @Getter
    private boolean checkMovement, accurateYawData, cinematicMode;
    @Getter
    @Setter
    private boolean excuseNextFlying;

    @Getter
    private final Timer lastTeleport = new TickTimer(), lastHighRate = new TickTimer();

    private int teleportsToConfirm;

    @Getter
    private final LinkedList<Float> yawGcdList = new EvictingList<>(45),
            pitchGcdList = new EvictingList<>(45);
    @Getter
    private float sensitivityX, sensitivityY, currentSensX, currentSensY, sensitivityMcp, yawMode, pitchMode;
    @Getter
    private int sensXPercent, sensYPercent;
    @Getter
    private TickTimer lastCinematic = new TickTimer(2);
    private MouseFilter mxaxis = new MouseFilter(), myaxis = new MouseFilter();
    private float smoothCamFilterX, smoothCamFilterY, smoothCamYaw, smoothCamPitch;
    private Timer lastReset = new TickTimer(2), generalProcess = new TickTimer(3);
    private final EvictingList<Integer> sensitivitySamples = new EvictingList<>(50);


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

        if (packet.isLooked()) {
            float deltaYaw = Math.abs(this.deltaYaw), lastDeltaYaw = Math.abs(this.lDeltaYaw);
            final double differenceYaw = Math.abs(this.deltaYaw - lastDeltaYaw);
            final double differencePitch = Math.abs(this.deltaPitch - this.lDeltaPitch);

            final double joltYaw = Math.abs(differenceYaw - deltaYaw);
            final double joltPitch = Math.abs(differencePitch - this.deltaPitch);

            final float yawThreshold = Math.max(1.0f, deltaYaw / 2f),
                    pitchThreshold = Math.max(1.f, Math.abs(this.deltaPitch) / 2f);

            if (joltYaw > yawThreshold && joltPitch > pitchThreshold) this.lastHighRate.reset();
            this.lastPitchGCD = this.pitchGCD;
            this.lastYawGCD = this.yawGCD;
            this.yawGCD = MathUtils
                    .gcdSmall(this.deltaYaw, this.lDeltaYaw);
            this.pitchGCD = MathUtils
                    .gcdSmall(this.deltaPitch, this.lDeltaPitch);

            val origin = this.to.getLoc().clone();

            origin.y+= player.getInfo().isSneaking() ? 1.54 : 1.62;

            if(lastTeleport.isPassed(1)) {
                predictionHandling:
                {
                    float yawGcd = this.yawGCD,
                            pitchGcd = this.pitchGCD;

                    //Adding gcd of yaw and pitch.
                    if (this.yawGCD > 0.01 && this.yawGCD < 1.2) {
                        yawGcdList.add(yawGcd);
                    }
                    if (this.pitchGCD > 0.01 && this.pitchGCD < 1.2)
                        pitchGcdList.add(pitchGcd);

                    if(yawGcdList.size() < 20 || pitchGcdList.size() < 20) {
                        accurateYawData = false;
                        break predictionHandling;
                    }

                    accurateYawData = true;

                    //Making sure to get shit within the std for a more accurate result.

                    //Making sure to get shit within the std for a more accurate result.
                    currentSensX = getSensitivityFromYawGCD(yawGcd);
                    currentSensY = getSensitivityFromPitchGCD(pitchGcd);
                    if (lastReset.isPassed()) {
                        pitchMode = MathUtils.getMode(pitchGcdList);
                        lastReset.reset();
                        sensXPercent = sensToPercent(sensitivityX = getSensitivityFromYawGCD(yawMode));
                        sensYPercent = sensToPercent(sensitivityY = getSensitivityFromPitchGCD(pitchMode));

                        table: {
                            sensitivitySamples.add(Math.max(sensXPercent, sensYPercent));

                            if (sensitivitySamples.size() > 30) {
                                final long mode = MathUtils.getMode(sensitivitySamples);

                                sensitivityMcp = AimbotUtil.SENSITIVITY_MAP.getOrDefault((int) mode, -1.0F);
                            }
                        }
                    }


                    lastLookX = lookX;
                    lastLookY = lookY;
                    lookX = getExpiermentalDeltaX(player);
                    lookY = getExpiermentalDeltaY(player);

                    if ((this.pitchGCD < 0.006 && this.yawGCD < 0.006) && smoothCamFilterY < 1E6
                            && smoothCamFilterX < 1E6 && player.getCreation().isPassed(1000L)) {
                        float sens = MovementHandler.percentToSens(95);
                        float f = sens * 0.6f + .2f;
                        float f1 = f * f * f * 8;
                        float f2 = lookX * f1;
                        float f3 = lookY * f1;

                        smoothCamFilterX = mxaxis.smooth(smoothCamYaw, .05f * f1);
                        smoothCamFilterY = myaxis.smooth(smoothCamPitch, .05f * f1);

                        this.smoothCamYaw += f2;
                        this.smoothCamPitch += f3;

                        f2 = smoothCamFilterX * 0.5f;
                        f3 = smoothCamFilterY * 0.5f;

                        //val clampedFrom = (Math.abs(this.from.yaw) > 360 ? this.from.yaw % 360 : this.from.yaw);
                        val clampedFrom = MathUtils.yawTo180F(getFrom().getLoc().yaw);
                        float pyaw = clampedFrom + f2 * .15f;
                        float ppitch = this.getFrom().getLoc().pitch - f3 * .15f;

                        this.lsmoothYaw = smoothYaw;
                        this.lsmoothPitch = smoothPitch;
                        this.smoothYaw = pyaw;
                        this.smoothPitch = ppitch;

                        float yaccel = Math.abs(this.deltaYaw) - Math.abs(this.lDeltaYaw),
                                pAccel = Math.abs(this.deltaPitch) - Math.abs(this.lDeltaPitch);

                        if (MathUtils.getDelta(smoothYaw, clampedFrom) > (yaccel > 0 ? (yaccel > 10 ? 2.5 : 1) : 0.3)
                                || MathUtils.getDelta(smoothPitch, this.getFrom().getLoc().pitch)
                                > (pAccel > 0 ? (pAccel > 10 ? 2.5 : 1) : 0.3)) {
                            smoothCamYaw = smoothCamPitch = 0;
                            this.cinematicMode = false;
                            mxaxis.reset();
                            myaxis.reset();
                        } else this.cinematicMode = true;
                    } else {
                        mxaxis.reset();
                        myaxis.reset();
                        this.cinematicMode = false;
                    }

                    lastLookX = lookX;
                    lastLookY = lookY;
                    lookX = getExpiermentalDeltaX(player);
                    lookY = getExpiermentalDeltaY(player);
                }
            } else {
                yawGcdList.clear();
                pitchGcdList.clear();
            }
        }

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
                || this.creative
                || hasLevi
                || data.excuseNextFlying
                || data.getPlayer().isSleeping()
                || (this.lastGhostCollision.isNotPassed() && this.lastBlockPlace.isPassed(2))
                || this.doingTeleport
                || this.lastTeleportTimer.isNotPassed(1)
                || this.riptiding
                || this.gliding
                || this.vehicleTimer.isNotPassed(3)
                || this.lastPlaceLiquid.isNotPassed(5)
                || this.inVehicle
                || ((this.lastChunkUnloaded.isNotPassed(35) || this.doingBlockUpdate)
                && MathUtils.getDelta(-0.098, this.deltaY) < 0.0001)
                || timeStamp - this.lastRespawn < 2500L
                || this.lastToggleFlight.isNotPassed(40)
                || timeStamp - data.creation < 4000
                || Kauri.INSTANCE.lastTickLag.isNotPassed(5);
         */
    }

    private static float getDeltaX(float yawDelta, float gcd) {
        return MathHelper.ceiling_float_int(yawDelta / gcd);
    }

    private static float getDeltaY(float pitchDelta, float gcd) {
        return MathHelper.ceiling_float_int(pitchDelta / gcd);
    }

    public static float getExpiermentalDeltaX(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaYaw();
        float sens = data.getMovement().sensitivityX;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        float result = deltaPitch / (calc * .15f);

        return result;
    }

    public static float getExpiermentalDeltaY(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaPitch();
        float sens = data.getMovement().sensitivityY;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        float result = deltaPitch / (calc * .15f);

        return result;
    }

    public static int sensToPercent(float sensitivity) {
        return MathHelper.floor_float(sensitivity / .5f * 100);
    }

    public static float percentToSens(int percent) {
        return percent * .0070422534f;
    }

    public static float getSensitivityFromYawGCD(float gcd) {
        return ((float) Math.cbrt(yawToF2(gcd) / 8f) - .2f) / .6f;
    }

    private static float getSensitivityFromPitchGCD(float gcd) {
        return ((float)Math.cbrt(pitchToF3(gcd) / 8f) - .2f) / .6f;
    }

    private static float getF1FromYaw(float gcd) {
        float f = getFFromYaw(gcd);

        return f * f * f * 8;
    }

    private static float getFFromYaw(float gcd) {
        float sens = getSensitivityFromYawGCD(gcd);
        return sens * .6f + .2f;
    }

    private static float getFFromPitch(float gcd) {
        float sens = getSensitivityFromPitchGCD(gcd);
        return sens * .6f + .2f;
    }

    private static float getF1FromPitch(float gcd) {
        float f = getFFromPitch(gcd);

        return (float)Math.pow(f, 3) * 8;
    }

    private static float yawToF2(float yawDelta) {
        return yawDelta / .15f;
    }

    private static float pitchToF3(float pitchDelta) {
        int b0 = pitchDelta >= 0 ? 1 : -1; //Checking for inverted mouse.
        return (pitchDelta / b0) / .15f;
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
        deltaYaw = to.getLoc().yaw - from.getLoc().yaw;
        deltaPitch = to.getLoc().pitch - from.getLoc().pitch;
    }
}
