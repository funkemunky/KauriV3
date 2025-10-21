package dev.brighten.ac.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.compat.CompatHandler;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.data.obj.CMove;
import dev.brighten.ac.platform.KauriPlayer;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.math.IntVector;
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
import me.hydro.emulator.object.iteration.Motion;
import me.hydro.emulator.object.result.IterationResult;
import me.hydro.emulator.util.PotionEffect;
import me.hydro.emulator.util.Vector;
import me.hydro.emulator.util.mcp.MathHelper;
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
    private final List<KLocation> posLocs = Collections.synchronizedList(new ArrayList<>());
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
    private double lastY, lastLastY, lastYawAcelleration, lastPitchAcelleration;
    @Getter
    private final Timer lastCinematic = new TickTimer(2);
    private final Timer lastReset = new TickTimer(2);
    private final EvictingList<Integer> sensitivitySamples = new EvictingList<>(50);
    private boolean modernMovement;

    public MovementHandler(APlayer player) {
        this.player = player;

        KauriPlayer bplayer = player.getBukkitPlayer();

        // Initializing player location
        to.setWorld(bplayer.getWorld());
        to.getLoc().setX(bplayer.getLocation().getX());
        to.getLoc().setY(bplayer.getLocation().getY());
        to.getLoc().setZ(bplayer.getLocation().getZ());
        to.getLoc().setYaw(bplayer.getLocation().getYaw());
        to.getLoc().setPitch(bplayer.getLocation().getPitch());
        to.setBox(new SimpleCollisionBox(to.getLoc(), 0.6, 1.8));
        to.setOnGround(bplayer.isOnGround());

        // Setting from as same location as to
        from.setLoc(to);

        modernMovement = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_5);
    }

    private final boolean[] IS_OR_NOT = new boolean[]{true, false};
    private final boolean[] ALWAYS_FALSE = new boolean[1];
    private final int[] FULL_RANGE = new int[]{-1, 0, 1};


    public void runEmulation(KLocation to, boolean isZeroThree) {
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

        if (player.EMULATOR.getTags().contains("003") && !isZeroThree) {
            runEmulation(to, true);
        }

        IterationResult minimum = null;
        iteration:
        {
            for (KLocation posLoc : new ArrayList<>(posLocs)) {
                // Resetting to prevent lag issues.

                IterationResult result = player.EMULATOR
                        .runTeleportIteration(new Vector(posLoc.getX(), posLoc.getY(), posLoc.getZ()));

                if (minimum == null || minimum.getOffset() > result.getOffset()) {
                    minimum = result;

                    if (minimum.getOffset() < 1E-26) {
                        // The player teleported, therefore we don't need to continue with predictions.
                        break iteration;
                    }
                }
            }

            List<Vector3d> possibleVelocity = new ArrayList<>();

            possibleVelocity.add(null);
            possibleVelocity.addAll(player.getVelocityHandler().getPossibleVectors());

            Motion previousMotion = player.EMULATOR.getMotion().clone();


            if(player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) {
                var playerInput = player.getInfo().getPlayerInput();
                int forward = 0, strafe = 0;

                if(playerInput.forward()) {
                    forward = 1;
                } else if(playerInput.backward()) {
                    forward = -1;
                }

                if(playerInput.left()) {
                    strafe = -1;
                } else if(playerInput.right()) {
                    strafe = 1;
                }
                for (boolean usingItem : getUsingItemIterations(forward, strafe)) {
                    for (boolean hitSlow : getHitSlowIterations()) {
                        for (FastMathType fastMath : getFastMathIterations(forward, strafe)) {
                            for (Vector3d possibleVector : possibleVelocity) {
                                IterationInput input = IterationInput.builder()
                                        .jumping(playerInput.jump())
                                        .forward(forward)
                                        .strafing(strafe)
                                        .sprinting(playerInput.sprint())
                                        .usingItem(usingItem)
                                        .modernMovement(modernMovement)
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
                                        .waitingForTeleport(!posLocs.isEmpty())
                                        .effectJump(EFFECTS[2]).build();

                                boolean isVelocity = false;
                                if (possibleVector != null) {
                                    // Setting the motion to the possible velocity vector.
                                    player.EMULATOR.getMotion().setMotionX(possibleVector.getX());
                                    player.EMULATOR.getMotion().setMotionY(possibleVector.getY());
                                    player.EMULATOR.getMotion().setMotionZ(possibleVector.getZ());
                                    // Has to be this way because order of operations in the emulator.
                                    isVelocity = true;
                                } else {
                                    // Resetting the motion to the previous motion.
                                    player.EMULATOR.getMotion().setMotionX(previousMotion.getMotionX());
                                    player.EMULATOR.getMotion().setMotionY(previousMotion.getMotionY());
                                    player.EMULATOR.getMotion().setMotionZ(previousMotion.getMotionZ());
                                }

                                IterationResult result = player.EMULATOR.runIteration(input);

                                if (isVelocity) {
                                    result.getTags().add("velocity");
                                }


                                if (fastMath == FastMathType.FAST_LEGACY) {
                                    result.getTags().add("fast_legacy");
                                } else if (fastMath == FastMathType.VANILLA) {
                                    result.getTags().add("vanilla");
                                } else if (fastMath == FastMathType.FAST_NEW) {
                                    result.getTags().add("fast_new");
                                } else if (fastMath == FastMathType.MODERN_VANILLA) {
                                    result.getTags().add("modern_vanilla");
                                }

                                if (forward > 0) {
                                    result.getTags().add("w-key");
                                } else if (forward < 0) {
                                    result.getTags().add("s-key");
                                }

                                if (strafe > 0) {
                                    result.getTags().add("d-key");
                                } else if (strafe < 0) {
                                    result.getTags().add("a-key");
                                }

                                if (minimum == null || minimum.getOffset() > result.getOffset()) {
                                    minimum = result;

                                    if (minimum.getOffset() < 1E-26) {
                                        break iteration;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                for (int forward : isZeroThree ? new int[]{0} : FULL_RANGE) {
                    for (int strafe : isZeroThree ? new int[]{0} : FULL_RANGE) {
                        for (boolean jumping : getJumpingIterations()) {
                            for (boolean sprinting : getSprintingIterations(forward)) {
                                for (boolean usingItem : getUsingItemIterations(forward, strafe)) {
                                    for (boolean hitSlow : getHitSlowIterations()) {
                                        for (FastMathType fastMath : getFastMathIterations(forward, strafe)) {
                                            for (Vector3d possibleVector : possibleVelocity) {
                                                IterationInput input = IterationInput.builder()
                                                        .jumping(jumping)
                                                        .forward(forward)
                                                        .strafing(strafe)
                                                        .sprinting(sprinting)
                                                        .usingItem(usingItem)
                                                        .modernMovement(modernMovement)
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
                                                        .waitingForTeleport(!posLocs.isEmpty())
                                                        .effectJump(EFFECTS[2]).build();

                                                boolean isVelocity = false;
                                                if (possibleVector != null) {
                                                    // Setting the motion to the possible velocity vector.
                                                    player.EMULATOR.getMotion().setMotionX(possibleVector.getX());
                                                    player.EMULATOR.getMotion().setMotionY(possibleVector.getY());
                                                    player.EMULATOR.getMotion().setMotionZ(possibleVector.getZ());
                                                    // Has to be this way because order of operations in the emulator.
                                                    isVelocity = true;
                                                } else {
                                                    // Resetting the motion to the previous motion.
                                                    player.EMULATOR.getMotion().setMotionX(previousMotion.getMotionX());
                                                    player.EMULATOR.getMotion().setMotionY(previousMotion.getMotionY());
                                                    player.EMULATOR.getMotion().setMotionZ(previousMotion.getMotionZ());
                                                }

                                                IterationResult result = player.EMULATOR.runIteration(input);

                                                if (isVelocity) {
                                                    result.getTags().add("velocity");
                                                }


                                                if (fastMath == FastMathType.FAST_LEGACY) {
                                                    result.getTags().add("fast_legacy");
                                                } else if (fastMath == FastMathType.VANILLA) {
                                                    result.getTags().add("vanilla");
                                                } else if (fastMath == FastMathType.FAST_NEW) {
                                                    result.getTags().add("fast_new");
                                                } else if (fastMath == FastMathType.MODERN_VANILLA) {
                                                    result.getTags().add("modern_vanilla");
                                                }

                                                if (forward > 0) {
                                                    result.getTags().add("w-key");
                                                } else if (forward < 0) {
                                                    result.getTags().add("s-key");
                                                }

                                                if (strafe > 0) {
                                                    result.getTags().add("d-key");
                                                } else if (strafe < 0) {
                                                    result.getTags().add("a-key");
                                                }

                                                if (minimum == null || minimum.getOffset() > result.getOffset()) {
                                                    minimum = result;

                                                    if (minimum.getOffset() < 1E-26) {
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
            }
        }
        if (minimum != null) {
            predicted = minimum.getPredicted();

            double mx = player.EMULATOR.getMotion().getMotionX();
            double my = player.EMULATOR.getMotion().getMotionY();
            double mz = player.EMULATOR.getMotion().getMotionZ();

            double total = mx * mx + my * my + mz * mz;

            if (total < 9E-4) {
                player.getInfo().lastCanceledFlying.reset();
                minimum.getTags().add("003");
            }

            if (minimum.getOffset() > 1E-7 && !isZeroThree) {
                minimum.getTags().add("bad_offset");
                minimum.getMotion().setMotionX(deltaX);
                minimum.getMotion().setMotionY(deltaY);
                minimum.getMotion().setMotionZ(deltaZ);
            }
            player.EMULATOR.confirm(minimum.getIteration());

            if (minimum.getTags().contains("003")) {
                player.EMULATOR.getTags().add("003");
            }

            if (minimum.getTags().contains("bad_offset")) {
                player.EMULATOR.setLastReportedBoundingBox(getTo().getBox().toNeo());
            }
        }
    }

    private FastMathType[] getFastMathIterations(int strafe, int forward) {
        // Because no movement is being applied, there is no angle calculation being done
        if (strafe == 0 && forward == 0) {
            return new FastMathType[]{FastMathType.FAST_LEGACY};
        }

        if (player.getPlayerVersion().isOlderThan(ClientVersion.V_1_16)) {
            return new FastMathType[]{
                    FastMathType.FAST_LEGACY,
                    FastMathType.VANILLA};
        } else {
            return new FastMathType[]{FastMathType.VANILLA, FastMathType.FAST_NEW, FastMathType.MODERN_VANILLA};
        }
    }

    private boolean[] getSprintingIterations(int forward) {
        return forward <= 0 || player.getInfo().isSneaking() ? ALWAYS_FALSE : IS_OR_NOT;
    }

    private boolean[] getHitSlowIterations() {
        return player.getInfo().lastAttack.isPassed(2) ? ALWAYS_FALSE : IS_OR_NOT;
    }

    private boolean[] getUsingItemIterations(int forward, int strafe) {
        return (forward == 0 && strafe == 0 ||
                !BlockUtils.isUsable(player.getBukkitPlayer().getItemInHand().getType())) ? ALWAYS_FALSE : IS_OR_NOT;
    }

    private boolean[] getJumpingIterations() {
        return IS_OR_NOT;
    }


    public void process(WrapperPlayClientPlayerFlying packet) {

        player.getPotionHandler().onFlying(packet);

        excuseNextFlying = packet.hasPositionChanged() && packet.hasRotationChanged()
                && packet.getLocation().getX() == to.getX()
                && packet.getLocation().getY() == to.getY()
                && packet.getLocation().getZ() == to.getZ()
                && player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_17);

        checkMovement = teleportsToConfirm == 0 && posLocs.isEmpty();

        if (checkMovement) {
            moveTicks++;
            if (!packet.hasPositionChanged()) moveTicks = 1;
        } else moveTicks = 0;

        if (excuseNextFlying) {
            return;
        }

        updateLocations(packet);

        runEmulation(to.getLoc(), false);

        checkForTeleports(packet);

        if (packet.hasPositionChanged()) {
            player.getBlockInfo().runCollisionCheck();
        }

        if (moveTicks > 0) {

            // Updating block locations
            player.getInfo().setBlockOnTo(Optional.of(player.getBlockUpdateHandler()
                    .getBlock(new IntVector(to.getLoc()))));
            player.getInfo().setBlockBelow(Optional.of(player.getBlockUpdateHandler()
                    .getBlock(new IntVector(to.getLoc().clone().subtract(0, 1, 0)))));

            if (packet.hasPositionChanged()) {
                // Updating player bounding box
                player.getInfo().getLastMove().reset();

                player.getInfo().setOnLadder(MovementUtils.isOnLadder(player));
            }

            if (packet.hasPositionChanged() && !lastTeleport.isNotPassed(2) && !player.getInfo().isCreative()
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

        if (packet.hasRotationChanged()) {
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

            origin.add(0, player.getInfo().isSneaking()
                    ? (player.getPlayerVersion().isOlderThan(ClientVersion.V_1_14) ? 1.54 : 1.27f) : 1.62, 0);

            RayCollision collision = new RayCollision(origin.toVector(), MathUtils.getDirection(origin));

            synchronized (lookingAtBoxes) {
                lookingAtBoxes.clear();
                lookingAtBoxes.addAll(collision
                        .boxesOnRay(player,
                                player.getBukkitPlayer().getGameMode().equals(GameMode.CREATIVE) ? 6.0 : 5.0));
                lookingAtBlock = !lookingAtBoxes.isEmpty();
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

                        // Creating a table of sensitivity
                        sensitivitySamples.add(Math.max(sensXPercent, sensYPercent));

                        if (sensitivitySamples.size() > 30) {
                            final long mode = MathUtils.getMode(sensitivitySamples);

                            sensitivityMcp = AimbotUtil.SENSITIVITY_MAP.getOrDefault((int) mode, -1.0F);
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

        boolean hasLevitation = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                && player.getPotionHandler().hasPotionEffect(XPotion.LEVITATION.getPotionEffectType());

        player.getInfo().setRiptiding(CompatHandler.getINSTANCE().isRiptiding(player.getBukkitPlayer()));
        player.getInfo().setGliding(CompatHandler.getINSTANCE().isGliding(player.getBukkitPlayer()));

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
        if (player.getInfo().isDoingVelocity()) {
            player.getInfo().getVelocity().reset();
        }
        synchronized (player.getInfo().getVelocityHistory()) {
            val iterator = player.getInfo().getVelocityHistory().iterator();
            while (iterator.hasNext()) {
                val velocity = iterator.next();

                if (Math.abs(velocity.getY() - getDeltaY()) < 0.01) {
                    player.getInfo().getVelocity().reset();
                    player.getInfo().setDoingVelocity(player.getVelocityHandler().getPossibleVectors().isEmpty());
                    player.getOnVelocityTasks().forEach(vectorConsumer -> vectorConsumer.accept(velocity));
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void processBotMove(WrapperPlayClientPlayerFlying packet) {
        if (player.getMob() == null) return;
        if (packet.hasPositionChanged() || packet.hasRotationChanged()) {
            KLocation origin = to.getLoc().clone().add(0, 1.7, 0);

            final double MULTIPLIER = Math.max(-0.5, Math.min(-1, -1 / (Math.abs(deltaYaw) * 0.25)));
            RayCollision coll = new RayCollision(origin.toVector(), origin.getDirection()
                    .multiply(MULTIPLIER).setY(0));

            Location loc1 = coll.collisionPoint(1.5).toLocation(player.getBukkitPlayer().getWorld());

            if (player.getInfo().botAttack.isNotPassed(7)) {
                loc1.setY(Math.max(origin.getY() + 2, loc1.getY()));
                player.getMob().teleport(loc1.getX(), loc1.getY(), loc1.getZ(), loc1.getYaw(), loc1.getPitch());
            } else {
                loc1.setY(Math.max(origin.getY() + 0.6, loc1.getY()));
                if (Math.random() > 0.2)
                    Anticheat.INSTANCE.getRunUtils().taskLaterAsync(() -> player.getMob()
                            .teleport(loc1.getX(), loc1.getY(), loc1.getZ(), loc1.getYaw(), loc1.getPitch()), 5);
            }
        }
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
        this.lastY = y;

        this.lastYawAcelleration = yawAcelleration;
        this.lastPitchAcelleration = pitchAcelleration;

        this.cinematic = this.ticks > 5;

        if (cinematic) lastCinematic.reset();
        sentPositionUpdate = false;
    }

    private final RelativeFlag relFlags = RelativeFlag.X.or(RelativeFlag.Y)
            .or(RelativeFlag.Z).or(RelativeFlag.YAW).or(RelativeFlag.PITCH);

    public void runPositionHackFix() {
        if (sentPositionUpdate) return;

        player.sendPacket(new WrapperPlayServerPlayerPositionAndLook(0, Vector3d.zero(), new Vector3d(0, 0, 0), 0, 0,
                relFlags));

        player.getBukkitPlayer().sendMessage("§c[Anticheat] §7Position update sent to fix movement issues.");

        sentPositionUpdate = true;
    }

    boolean isInvalidGCD() {
        return pitchGCD < 0.0078125;
    }

    public float getExperimentalDeltaX(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaYaw();
        float sens = data.getMovement().sensitivityX;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        return deltaPitch / (calc * .15f);
    }

    public double[] getEyeHeights() {
        if (player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new double[]{0.4f, 1.27f, 1.62f};
        } else if (player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return new double[]{0.4f, 1.54f, 1.62f};
        } else return new double[]{1.54f, 1.62f};
    }

    public float getExperimentalDeltaY(APlayer data) {
        float deltaPitch = data.getMovement().getDeltaPitch();
        float sens = data.getMovement().sensitivityY;
        float f = sens * 0.6f + .2f;
        float calc = f * f * f * 8;

        return deltaPitch / (calc * .15f);
    }

    public int sensToPercent(float sensitivity) {
        return MathHelper.floor_float(sensitivity / .5f * 100);
    }

    public float getSensitivityFromYawGCD(float gcd) {
        return ((float) Math.cbrt(yawToF2(gcd) / 8f) - .2f) / .6f;
    }

    private float getSensitivityFromPitchGCD(float gcd) {
        return ((float) Math.cbrt(pitchToF3(gcd) / 8f) - .2f) / .6f;
    }

    private float yawToF2(float yawDelta) {
        return yawDelta / .15f;
    }

    private float pitchToF3(float pitchDelta) {
        int b0 = pitchDelta >= 0 ? 1 : -1; //Checking for inverted mouse.
        return (pitchDelta / b0) / .15f;
    }

    public void addPosition(WrapperPlayServerPlayerPositionAndLook packet) {
        final KLocation loc = new KLocation(packet.getX(), packet.getY(), packet.getZ(),
                packet.getYaw(), packet.getPitch());

        if (packet.getRelativeFlags().has(RelativeFlag.X)) {
            loc.add(player.getMovement().getTo().getLoc().getX(), 0, 0);
        }
        if (packet.getRelativeFlags().has(RelativeFlag.Y)) {
            loc.add(0, player.getMovement().getTo().getLoc().getY(), 0);
        }
        if (packet.getRelativeFlags().has(RelativeFlag.Z)) {
            loc.add(0, 0, player.getMovement().getTo().getLoc().getZ());
        }
        if (packet.getRelativeFlags().has(RelativeFlag.YAW)) {
            loc.setPitch(loc.getPitch() + player.getMovement().getTo().getLoc().getPitch());
        }
        if (packet.getRelativeFlags().has(RelativeFlag.PITCH)) {
            loc.setYaw(loc.getYaw() + player.getMovement().getTo().getLoc().getYaw());
        }

        teleportsToConfirm++;

        loc.setTimeStamp(System.currentTimeMillis());

        player.runKeepaliveAction(ka -> {
            teleportsToConfirm--;

            synchronized (posLocs) {
                posLocs.remove(loc);
            }
        }, 2);
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
        KLocation newLoc = new KLocation(location);
        to.getLoc().setLocation(newLoc);
        to.getLoc().setLocation(newLoc);

        deltaX = deltaY = deltaZ = deltaXZ
                = lDeltaX = lDeltaY = lDeltaZ
                = lDeltaXZ = 0;

        deltaYaw = lDeltaYaw =
                deltaPitch = lDeltaPitch = 0;
        moveTicks = 0;
        //doingTeleport = inventoryOpen  = false;
    }

    private void checkForTeleports(WrapperPlayClientPlayerFlying packet) {
        if (packet.hasPositionChanged() && packet.hasRotationChanged() && !packet.isOnGround()) {
            synchronized (posLocs) {
                Iterator<KLocation> iterator = posLocs.iterator();

                //Iterating through the ArrayList to find a potential teleport. We can't remove from the list
                //without causing a CME unless we use Iterator#remove().
                while (iterator.hasNext()) {
                    KLocation posLoc = iterator.next();

                    KLocation to = new KLocation(packet.getLocation().getX(),
                            packet.getLocation().getY(),
                            packet.getLocation().getZ());
                    double distance = MathUtils.getDistanceWithoutRoot(to, posLoc);

                    if (distance < 1E-9) {
                        lastTeleport.reset();
                        from.setLoc(this.to);
                        iterator.remove();
                        break;
                    }
                }

                //Ensuring the list doesn't overflow with old locations, a potential crash exploit.
                if (teleportsToConfirm == 0 && !posLocs.isEmpty()) {
                    posLocs.clear();
                }
            }
        }
    }

    /**
     * Setting the to and from to current location only if the player either moved or looked.
     *
     * @param packet WrapperPlayClientPlayerFlyingh
     */
    private void setTo(WrapperPlayClientPlayerFlying packet) {
        to.setWorld(player.getBukkitPlayer().getWorld());
        if (packet.hasPositionChanged()) {
            to.getLoc().setX(packet.getLocation().getX());
            to.getLoc().setY(packet.getLocation().getY());
            to.getLoc().setZ(packet.getLocation().getZ());
        }
        if (packet.hasRotationChanged()) {
            to.getLoc().setYaw(packet.getLocation().getYaw());
            to.getLoc().setPitch(packet.getLocation().getPitch());
        }
        to.setBox(new SimpleCollisionBox(to.getLoc(), 0.6, 1.8));
        to.setOnGround(packet.isOnGround());
    }

    /**
     * If from location is null, update to loc after to is set, otherwise, update to before from.
     * Updates the location of player and its general delta
     *
     * @param packet WrapperPlayClientPlayerFlying
     */
    private void updateLocations(WrapperPlayClientPlayerFlying packet) {
        if (to.getBox().max().lengthSquared() == 0) { //Needs initializing
            setTo(packet);
            from.setLoc(to);
            player.getBukkitPlayer().sendMessage("Initializing location");
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

        deltaX = to.getLoc().getX() - from.getLoc().getX();
        deltaY = to.getLoc().getY() - from.getLoc().getY();
        deltaZ = to.getLoc().getZ() - from.getLoc().getZ();
        deltaXZ = Math.hypot(deltaX, deltaZ); // Calculating here to cache since hypot() can be heavy.
        deltaYaw = to.getLoc().getYaw() - from.getLoc().getYaw();
        deltaPitch = to.getLoc().getPitch() - from.getLoc().getPitch();

        player.getInfo().setClientGroundTicks(packet.isOnGround() ? player.getInfo().getClientGroundTicks() + 1 : 0);
        player.getInfo().setClientAirTicks(!packet.isOnGround() ? player.getInfo().getClientAirTicks() + 1 : 0);
    }
}
