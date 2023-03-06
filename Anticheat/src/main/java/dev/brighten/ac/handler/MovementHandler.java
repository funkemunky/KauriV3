package dev.brighten.ac.handler;

import com.google.common.collect.Sets;
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
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import me.hydro.emulator.object.input.IterationInput;
import me.hydro.emulator.object.result.IterationResult;
import me.hydro.emulator.util.PotionEffect;
import me.hydro.emulator.util.Vector;
import me.hydro.emulator.util.mcp.MathHelper.FastMathType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class MovementHandler {

    private final APlayer player;


    @Getter
    private final CMove to = new CMove(), from = new CMove();
    @Getter
    private double deltaX, deltaY, deltaZ, deltaXZ,
            lDeltaX, lDeltaY, lDeltaZ, lDeltaXZ;

    @Getter
    private Vector predicted;
    @Getter
    private float lookX, lookY, lastLookX, lastLookY;
    @Getter
    private float deltaYaw, deltaPitch, lDeltaYaw, lDeltaPitch, pitchGCD, lastPitchGCD, yawGCD, lastYawGCD;
    @Getter
    private int moveTicks;
    @Getter
    private final List<KLocation> posLocs = new ArrayList<>();
    @Getter
    private final List<CollisionBox> lookingAtBoxes = new ArrayList<>();
    @Getter
    private boolean checkMovement, accurateYawData, cinematic, jumped, inAir, lookingAtBlock;
    @Getter
    @Setter
    private boolean excuseNextFlying;

    private boolean sentPositionUpdate;

    @Getter
    private final Timer lastTeleport = new TickTimer(), lastHighRate = new TickTimer(),
            lastFlying = new TickTimer();

    @Getter
    private int teleportsToConfirm;

    @Getter
    private final LinkedList<Float> yawGcdList = new EvictingList<>(45),
            pitchGcdList = new EvictingList<>(45);
    @Getter
    private float sensitivityX, sensitivityY, currentSensX, currentSensY, sensitivityMcp, yawMode, pitchMode;
    @Getter
    private int sensXPercent, sensYPercent, airTicks, groundTicks;
    private int ticks;
    private double lastX, lastY, lastLastY, lastYawAcelleration, lastPitchAcelleration;
    @Getter
    private final Timer lastCinematic = new TickTimer(2);
    private final Timer lastReset = new TickTimer(2);
    private final EvictingList<Integer> sensitivitySamples = new EvictingList<>(50);

    public MovementHandler(APlayer player) {
        this.player = player;

        Player bplayer = player.getBukkitPlayer();

        // Initializing player location
        to.setWorld(bplayer.getWorld());
        to.getLoc().x = bplayer.getLocation().getX();
        to.getLoc().y = bplayer.getLocation().getY();
        to.getLoc().z = bplayer.getLocation().getZ();
        to.getLoc().yaw = bplayer.getLocation().getYaw();
        to.getLoc().pitch = bplayer.getLocation().getPitch();
        to.setBox(new SimpleCollisionBox(to.getLoc(), 0.6, 1.8));
        to.setOnGround(bplayer.isOnGround());

        // Setting from as same location as to
        from.setLoc(to);
    }

    private static final boolean[] IS_OR_NOT = new boolean[]{true, false};
    private static final boolean[] ALWAYS_FALSE = new boolean[1];
    private static final int[] FULL_RANGE = new int[]{-1, 0, 1};

    public void runEmulation(WPacketPlayInFlying packet) {
        /*
         * (org.bukkit.potion.PotionEffectType
         * Element 0: SPEED
         * Element 1: SLOW
         * Element 2: JUMP
         */
        final PotionEffect[] EFFECTS = new PotionEffect[3];

        for (org.bukkit.potion.PotionEffect potionEffect : player.getPotionHandler().potionEffects) {
            if (potionEffect.getType().equals(PotionEffectType.SPEED)) {
                EFFECTS[0] = PotionEffect.builder()
                        .amplifier(potionEffect.getAmplifier())
                        .type(me.hydro.emulator.util.PotionEffectType.SPEED)
                        .build();
            } else if (potionEffect.getType().equals(PotionEffectType.SLOW)) {
                EFFECTS[1] = PotionEffect.builder()
                        .amplifier(potionEffect.getAmplifier())
                        .type(me.hydro.emulator.util.PotionEffectType.SLOW)
                        .build();
            } else if (potionEffect.getType().equals(PotionEffectType.JUMP)) {
                EFFECTS[2] = PotionEffect.builder()
                        .amplifier(potionEffect.getAmplifier())
                        .type(me.hydro.emulator.util.PotionEffectType.JUMP)
                        .build();
            }
        }

        IterationResult minimum = null;
        iteration: {
            for (KLocation posLoc : posLocs) {
                IterationResult result = player.EMULATOR.runTeleportIteration(new Vector(posLoc.x, posLoc.y, posLoc.z));

                if (minimum == null || minimum.getOffset() > result.getOffset()) {
                    minimum = result;

                    if(minimum.getOffset() < 1E-26) {
                        // The player teleported, therefore we don't need to continue with predictions.
                        break iteration;
                    }
                }
            }

            for (int forward : FULL_RANGE) {
                for (int strafe : FULL_RANGE) {
                    for (boolean jumping : getJumpingIterations()) {
                        for (boolean usingItem : getUsingItemIterations(forward, strafe)) {
                            for (boolean sprinting : getSprintingIterations(forward)) {
                                for (boolean hitSlow : (player.getInfo().lastAttack.isNotPassed(1)
                                        ? IS_OR_NOT : ALWAYS_FALSE)) {
                                    for (FastMathType fastMath : getFastMathIterations()) {
                                        IterationInput input = IterationInput.builder()
                                                .jumping(jumping)
                                                .forward(forward)
                                                .strafing(strafe)
                                                .sprinting(sprinting)
                                                .usingItem(usingItem)
                                                .hitSlowdown(hitSlow)
                                                .aiMoveSpeed(player.getBukkitPlayer().getWalkSpeed() / 2)
                                                .fastMathType(fastMath)
                                                .sneaking(player.getInfo().isSneaking())
                                                .ground(from.isOnGround())
                                                .to(new Vector(to.getX(), to.getY(), to.getZ()))
                                                .yaw(to.getYaw())
                                                .lastReportedBoundingBox(from.getBox().toNeo())
                                                .effectSpeed(EFFECTS[0])
                                                .effectSlow(EFFECTS[1])
                                                .effectJump(EFFECTS[2])
                                                .build();

                                        IterationResult result = player.EMULATOR.runIteration(input);

                                        if (minimum == null || minimum.getOffset() > result.getOffset()) {
                                            minimum = result;

                                            if(minimum.getOffset() < 1E-26) {
                                                break iteration;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(minimum != null) {
            predicted = minimum.getPredicted();
            if (minimum.getOffset() > 1E-8) {
                minimum.getTags().add("bad_offset");
                minimum.getMotion().setMotionX(deltaX);
                minimum.getMotion().setMotionY(deltaY);
                minimum.getMotion().setMotionZ(deltaZ);
            }
            player.EMULATOR.confirm(minimum.getIteration());
        }
    }

    private FastMathType[] getFastMathIterations() {
        if (player.getPlayerVersion().isBelow(ProtocolVersion.V1_16)) {
            return new FastMathType[]{
                    FastMathType.FAST_LEGACY,
                    FastMathType.VANILLA};
        } else {
            return new FastMathType[]{FastMathType.VANILLA, FastMathType.FAST_NEW};
        }
    }

    private boolean[] getSprintingIterations(int forward) {
        if (player.getInfo().isSneaking())
            return ALWAYS_FALSE;

        return IS_OR_NOT;
    }

    private boolean[] getUsingItemIterations(int forward, int strafe) {
        return forward == 0 && strafe == 0 ? ALWAYS_FALSE : IS_OR_NOT;
    }

    private boolean[] getJumpingIterations() {
        return IS_OR_NOT;
    }


    public void process(WPacketPlayInFlying packet) {

        player.getPotionHandler().onFlying(packet);

        excuseNextFlying = packet.isMoved() && packet.isLooked()
                && packet.getX() == to.getX()
                && packet.getY() == to.getY()
                && packet.getZ() == to.getZ()
                && player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_17);

        checkMovement = MovementUtils.checkMovement(player.getPlayerConnection());

        if (checkMovement) {
            moveTicks++;
            if (!packet.isMoved()) moveTicks = 1;
        } else moveTicks = 0;

        if(excuseNextFlying) {
            return;
        }

        updateLocations(packet);

        if (packet.isMoved()) {
            player.getBlockInfo().runCollisionCheck();
        }

        runEmulation(packet);

        if (moveTicks > 0) {

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

            if (packet.isMoved() && !lastTeleport.isNotPassed(2) && !player.getInfo().isCreative()
                    && !player.getInfo().isCanFly()) {

                synchronized (player.pastLocations) { //To prevent ConcurrentModificationExceptions
                    player.pastLocations.add(new Tuple<>(getTo().getLoc().clone(),
                            deltaXZ + Math.abs(deltaY)));
                }
            }

            if (player.getBlockInfo().blocksAbove) {
                player.getInfo().getBlockAbove().reset();
            }
        }

        processVelocity();

        if (player.getBlockInfo().onSlime) player.getInfo().slimeTimer.reset();
        if (player.getBlockInfo().onClimbable) player.getInfo().climbTimer.reset();

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

            origin.y += player.getInfo().isSneaking()
                    ? (player.getPlayerVersion().isBelow(ProtocolVersion.V1_14) ? 1.54 : 1.27f) : 1.62;

            RayCollision collision = new RayCollision(origin.toVector(), MathUtils.getDirection(origin));

            synchronized (lookingAtBoxes) {
                lookingAtBoxes.clear();
                lookingAtBoxes.addAll(collision
                        .boxesOnRay(player.getBukkitPlayer().getWorld(),
                                player.getBukkitPlayer().getGameMode().equals(GameMode.CREATIVE) ? 6.0 : 5.0));
                lookingAtBlock = lookingAtBoxes.size() > 0;
            }

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
                    currentSensX = getSensitivityFromYawGCD(yawGcd);
                    currentSensY = getSensitivityFromPitchGCD(pitchGcd);
                    if (lastReset.isPassed()) {
                        yawMode = MathUtils.getMode(yawGcdList);
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
                    lookX = getExperimentalDeltaX(player);
                    lookY = getExperimentalDeltaY(player);

                    lastLookX = lookX;
                    lastLookY = lookY;
                    lookX = getExperimentalDeltaX(player);
                    lookY = getExperimentalDeltaY(player);
                }
            } else {
                yawGcdList.clear();
                pitchGcdList.clear();
            }
        }

        if (packet.isOnGround()) {
            player.getInfo().setWasOnSlime(player.getBlockInfo().onSlime);
            groundTicks++;
            airTicks = 0;
            player.getInfo().groundJumpBoost = player.getPotionHandler().getEffectByType(PotionEffectType.JUMP);
        } else {
            player.getInfo().groundJumpBoost = Optional.empty();
            airTicks++;
            groundTicks = 0;
        }

        player.getInfo().setCreative(player.getBukkitPlayer().getGameMode() == GameMode.CREATIVE
                || player.getBukkitPlayer().getGameMode() == GameMode.SPECTATOR
                || player.getInfo().getPossibleCapabilities().stream()
                .anyMatch(capability -> capability.canInstantlyBuild));

        player.getInfo().setCanFly(player.getBukkitPlayer().getAllowFlight()
                || player.getInfo().getPossibleCapabilities().stream()
                .anyMatch(capability -> capability.canFly));

        boolean hasLevitation = ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)
                && player.getPotionHandler().hasPotionEffect(XPotion.LEVITATION.getPotionEffectType());

        player.getInfo().setRiptiding(CompatHandler.getInstance().isRiptiding(player.getBukkitPlayer()));
        player.getInfo().setGliding(CompatHandler.getInstance().isGliding(player.getBukkitPlayer()));

        // Resetting glide/sneak timers
        if (player.getInfo().isGliding()) player.getInfo().getLastElytra().reset();
        if (player.getInfo().isSneaking()) player.getInfo().getLastSneak().reset();
        if (player.getBlockInfo().inLiquid) player.getInfo().getLastLiquid().reset();
        if (player.getBlockInfo().inWeb) player.getInfo().lastWeb.reset();
        if (player.getBlockInfo().onHalfBlock) player.getInfo().getLastHalfBlock().reset();
        if (player.getBlockInfo().fenceBelow) player.getInfo().getLastFence().reset();

        if (!to.isOnGround() && moveTicks > 0) {
            if (!jumped && from.isOnGround()
                    && deltaY >= 0) {
                jumped = true;
            } else {
                inAir = true;
                jumped = false;
            }
        } else jumped = inAir = false;

        player.getInfo().setGeneralCancel(player.getBukkitPlayer().getAllowFlight()
                || excuseNextFlying
                || lastTeleport.isNotPassed(0)
                || player.getBukkitPlayer().isFlying()
                || player.getInfo().isCanFly()
                || player.getInfo().isCreative()
                || player.getInfo().isInVehicle()
                || player.getInfo().getVehicleSwitch().isNotPassed(1)
                || player.getBukkitPlayer().isSleeping()
                || player.getInfo().isGliding()
                || player.getInfo().isRiptiding()
                || hasLevitation);

        lastFlying.reset();

        processBotMove(packet);

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

                if (Math.abs(velocity.getY() - getDeltaY()) < 0.01) {
                    player.getInfo().getVelocity().reset();
                    player.getInfo().setDoingVelocity(false);
                    player.getOnVelocityTasks().forEach(vectorConsumer -> vectorConsumer.accept(velocity));
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void processBotMove(WPacketPlayInFlying packet) {
        if (packet.isMoved() || packet.isLooked()) {
            KLocation origin = to.getLoc().clone().add(0, 1.7, 0);

            RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection().multiply(-1));

            Location loc1 = coll.collisionPoint(2.2).toLocation(player.getBukkitPlayer().getWorld());

            if (player.getInfo().botAttack.isNotPassed(7)) {
                loc1.setY(Math.max(origin.y + 1.2, loc1.getY()));
            } else loc1.setY(Math.max(origin.y + 0.3, loc1.getY()));

            player.getMob().teleport(loc1.getX(), loc1.getY(), loc1.getZ(), loc1.getYaw(), loc1.getPitch());
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

        if (cinematic) lastCinematic.reset();
        sentPositionUpdate = false;
    }

    private static final Set<WPacketPlayOutPosition.EnumPlayerTeleportFlags>
            relFlags = Sets.newHashSet(WPacketPlayOutPosition.EnumPlayerTeleportFlags.X,
            WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y,
            WPacketPlayOutPosition.EnumPlayerTeleportFlags.Z,
            WPacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT,
            WPacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);

    public void runPositionHackFix() {
        if (sentPositionUpdate) return;

        player.sendPacket(WPacketPlayOutPosition.builder().x(0).y(0).z(0).yaw(0).pitch(0).flags(relFlags)
                .build());
        sentPositionUpdate = true;
    }

    boolean isInvalidGCD() {
        return pitchGCD < 0.0078125;
    }

    public static float getExperimentalDeltaX(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaYaw();
        float sens = data.getMovement().sensitivityX;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        return deltaPitch / (calc * .15f);
    }

    public float getExperimentalDelta(float deltaAngle) {
        float sens = player.getMovement().sensitivityMcp;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        return deltaAngle / (calc * .15f);
    }

    public double[] getEyeHeights() {
        if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_14)) {
            return new double[]{0.4f, 1.27f, 1.62f};
        } else if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
            return new double[]{0.4f, 1.54f, 1.62f};
        } else return new double[]{1.54f, 1.62f};
    }

    public static float getExperimentalDeltaY(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaPitch();
        float sens = data.getMovement().sensitivityY;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        return deltaPitch / (calc * .15f);
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
     * Updates the location of player and its general delta
     *
     * @param packet WPacketPlayInFlying
     */
    private void updateLocations(WPacketPlayInFlying packet) {
        if (to.getBox().max().lengthSquared() == 0) { //Needs initializing
            setTo(packet);
            from.setLoc(to);
        } else {
            from.setLoc(to);
            setTo(packet);
        }

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

        player.getInfo().setClientGroundTicks(packet.isOnGround() ? player.getInfo().getClientGroundTicks() + 1 : 0);
        player.getInfo().setClientAirTicks(!packet.isOnGround() ? player.getInfo().getClientAirTicks() + 1 : 0);
    }
}
