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
    private float deltaYaw, deltaPitch, lDeltaYaw, lDeltaPitch, pitchGCD, lastPitchGCD, yawGCD, lastYawGCD;
    @Getter
    private int moveTicks;
    private final List<KLocation> posLocs = new ArrayList<>();
    @Getter
    private boolean checkMovement, accurateYawData, cinematic;
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
    private int ticks;
    private double lastX, lastY, lastLastY, lastYawAcelleration, lastPitchAcelleration;
    private boolean inTick;
    @Getter
    private TickTimer lastCinematic = new TickTimer(2);
    private Timer lastReset = new TickTimer(2), generalProcess = new TickTimer(3);
    private final EvictingList<Integer> sensitivitySamples = new EvictingList<>(50);


    public void process(WPacketPlayInFlying packet, long currentTime) {

        player.getPotionHandler().onFlying(packet);

        checkMovement = MovementUtils.checkMovement(player.getPlayerConnection());

        if (checkMovement) {
            moveTicks++;
        } else moveTicks = 0;

        if (moveTicks > 0) {
            updateLocations(packet);

            // Updating block locations
            player.getInfo().setBlockOnTo(BlockUtils
                    .getBlockAsync(to.getLoc().toLocation(player.getBukkitPlayer().getWorld())));
            player.getInfo().setBlockBelow(BlockUtils
                    .getBlockAsync(to.getLoc().toLocation(player.getBukkitPlayer().getWorld())
                            .subtract(0, 1, 0)));

            if (packet.isMoved()) {
                // Updating player bounding box
                player.getInfo().getLastMove().reset();

                player.getInfo().setOnLadder(MovementUtils.isOnLadder(player));
            }

            player.getBlockInformation().runCollisionCheck();

            if(player.getBlockInformation().blocksAbove) {
                player.getInfo().getBlockAbove().reset();
            }
        }

        processVelocity();

        checkForTeleports(packet);

        if (packet.isLooked()) {
            process();
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

            origin.y += player.getInfo().isSneaking() ? 1.54 : 1.62;

            if (lastTeleport.isPassed(1)) {
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

                    if (yawGcdList.size() < 20 || pitchGcdList.size() < 20) {
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

                        table:
                        {
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

        if (player.getInfo().isServerGround()) {
            player.getInfo().setWasOnSlime(player.getBlockInformation().onSlime);
        }

        player.getInfo().setCreative(player.getBukkitPlayer().getGameMode() == GameMode.CREATIVE
                || player.getBukkitPlayer().getGameMode() == GameMode.SPECTATOR);

        boolean hasLevitation = ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)
                && player.getPotionHandler().hasPotionEffect(XPotion.LEVITATION.getPotionEffectType());

        player.getInfo().setRiptiding(CompatHandler.getInstance().isRiptiding(player.getBukkitPlayer()));
        player.getInfo().setGliding(CompatHandler.getInstance().isGliding(player.getBukkitPlayer()));

        // Resetting glide/sneak timers
        if (player.getInfo().isGliding()) player.getInfo().getLastElytra().reset();
        if (player.getInfo().isSneaking()) player.getInfo().getLastSneak().reset();

        player.getInfo().setGeneralCancel(player.getBukkitPlayer().getAllowFlight()
                || moveTicks == 0
                || excuseNextFlying
                || player.getInfo().isCreative()
                || lastTeleport.isNotPassed(2)
                || teleportsToConfirm > 0
                || player.getInfo().isInVehicle()
                || player.getInfo().getVehicleSwitch().isNotPassed(1)
                || player.getBukkitPlayer().isSleeping()
                || player.getInfo().isGliding()
                || player.getInfo().isRiptiding()
                || hasLevitation);

        /*
        ata.playerInfo.generalCancel = data.getPlayer().getAllowFlight()
                || this.creativelastLastY
                || hasLeviit
it
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
    // generate a method that processes velocityHistory and compares to current deltaY.
    private void processVelocity() {
        //Iterate through player.getInfo().getVelocityHistory() and compare to current deltaY.
        synchronized (player.getInfo().getVelocityHistory()) {
            val iterator = player.getInfo().getVelocityHistory().iterator();
            while (iterator.hasNext()) {
                val velocity = iterator.next();

                if(Math.abs(velocity.getY() - getDeltaY()) < 0.01) {
                    player.getInfo().getVelocity().reset();
                    player.getInfo().setDoingVelocity(false);
                    player.getOnVelocityTasks().forEach(vectorConsumer -> vectorConsumer.accept(velocity));
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private static float getDeltaX(float yawDelta, float gcd) {
        return MathHelper.ceiling_float_int(yawDelta / gcd);
    }

    private static float getDeltaY(float pitchDelta, float gcd) {
        return MathHelper.ceiling_float_int(pitchDelta / gcd);
    }
    public void process() {

        float yawAcelleration = Math.abs(getDeltaYaw());
        float pitchAcelleration = Math.abs(getDeltaPitch());

        // They are not rotating
        if (yawAcelleration < 0.002 || pitchAcelleration < 0.002) return;

        // Deltas between the current acelleration and last
        double x = Math.abs(yawAcelleration - this.lastYawAcelleration);
        double y = Math.abs(pitchAcelleration - this.lastPitchAcelleration);

        // Deltas between last X & Y
        double deltaX = Math.abs(x - this.lastX);
        double deltaY = Math.abs(y - this.lastY);

        // Pitch delta change
        double pitchChangeAcelleration = Math.abs(this.lastLastY - deltaY);
        this.inTick = false;

        // we have to check something different for pitch due to it being a little harder to check for being smooth
        if (x < .04 || y < .04
                || (pitchAcelleration > .08 && pitchChangeAcelleration > 0
                && !MathUtils.isScientificNotation(pitchChangeAcelleration)
                && pitchChangeAcelleration < .0855)) {

            if (this.isInvalidGCD()) {
                this.ticks += (this.ticks < 20 ? 1 : 0);
            }
        } else {
            this.ticks -= this.ticks > 0 ? 1 : 0;
        }

        this.lastLastY = deltaY;
        this.lastX = x;
        this.lastY = y;

        this.lastYawAcelleration = yawAcelleration;
        this.lastPitchAcelleration = pitchAcelleration;

        this.cinematic = this.ticks > 5;

        if(cinematic) lastCinematic.reset();
    }

    boolean isInvalidGCD() {
        return pitchGCD < 0.0078125;
    }

    public static float getExpiermentalDeltaX(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaYaw();
        float sens = data.getMovement().sensitivityX;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        float result = deltaPitch / (calc * .15f);

        return result;
    }

    public double[] getEyeHeights() {
        if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_14)) {
            return new double[]{0.4f, 1.27f, 1.62f};
        } else if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
            return new double[]{0.4f, 1.54f, 1.62f};
        } else return new double[]{1.54f, 1.62f};
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
        return ((float) Math.cbrt(pitchToF3(gcd) / 8f) - .2f) / .6f;
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

        return (float) Math.pow(f, 3) * 8;
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

        teleportsToConfirm++;

        player.runKeepaliveAction(ka -> teleportsToConfirm--, 2);
        synchronized (posLocs) {
            posLocs.add(loc);
        }
    }

    /**
     * Updating the "to" and "from" location to current location.
     * Resetting position tracking; meant primarily for instant teleports.
     *
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
        if (packet.isMoved() && packet.isLooked() && !packet.isOnGround()) {
            synchronized (posLocs) {
                Iterator<KLocation> iterator = posLocs.iterator();

                //Iterating through the ArrayList to find a potential teleport. We can't remove from the list
                //without causing a CME unless we use Iterator#remove().
                while (iterator.hasNext()) {
                    KLocation posLoc = iterator.next();

                    KLocation to = new KLocation(packet.getX(), packet.getY(), packet.getZ());
                    double distance = MathUtils.getDistanceWithoutRoot(to, posLoc);

                    if (distance < 1E-9) {
                        lastTeleport.reset();
                        iterator.remove();
                        break;
                    }
                }

                //Ensuring the list doesn't overflow with old locations, a potential crash exploit.
                if (teleportsToConfirm == 0 && posLocs.size() > 0) {
                    posLocs.clear();
                }
            }
        }
    }

    /**
     * Setting the to and from to current location only if the player either moved or looked.
     *
     * @param packet WPacketPlayInFlyingh
     */
    private void setTo(WPacketPlayInFlying packet) {
        to.setWorld(player.getBukkitPlayer().getWorld());
        if (packet.isMoved()) {
            to.getLoc().x = packet.getX();
            to.getLoc().y = packet.getY();
            to.getLoc().z = packet.getZ();
        }
        if (packet.isLooked()) {
            to.getLoc().yaw = packet.getYaw();
            to.getLoc().pitch = packet.getPitch();
        }
        to.setBox(new SimpleCollisionBox(to.getLoc(), 0.6, 1.8));
        to.setOnGround(packet.isOnGround());
    }

    /**
     * If from location is null, update to loc after to is set, otherwise, update to before from.
     * Updates the location of player and its general delta movement.
     *
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
