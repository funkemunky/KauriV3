package dev.brighten.ac.data.info;

import com.github.retrooper.packetevents.util.Vector3d;
import dev.brighten.ac.data.obj.Pose;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.packet.PlayerCapabilities;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.KPotionEffect;
import dev.brighten.ac.utils.PastLocation;
import dev.brighten.ac.utils.math.RollingAverage;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GeneralInformation {
    public Optional<WrappedBlock> blockOnTo = Optional.empty(), blockBelow = Optional.empty();
    public Timer lastMove = new TickTimer(), vehicleSwitch = new TickTimer(), lastAbilities = new TickTimer(),
            lastSneak = new TickTimer(), velocity = new TickTimer(), lastCancel = new TickTimer(),
            slimeTimer = new TickTimer(), lastElytra = new TickTimer(), blockAbove = new TickTimer(),
            lastPlace = new TickTimer(), climbTimer = new TickTimer(), lastUseItem = new TickTimer(),
            lastRespawn = new TickTimer(), lastEntityCollision = new TickTimer(), lastWeb = new TickTimer(),
            lastLiquid = new TickTimer(), lastBlockDig = new TickTimer(), lastBlockPlace = new TickTimer(),
            lastBlockUpdate = new TickTimer(), lastMiscNear = new TickTimer(), lastHalfBlock = new TickTimer(),
            lastFence = new TickTimer(), lastFakeBotHit = new TickTimer(), lastInventoryOpen = new TickTimer(),
            botAttack = new TickTimer(), lastAttack = new TickTimer(), lastCanceledFlying = new TickTimer();
    public TrackedEntity target;
    public Optional<KPotionEffect> groundJumpBoost;
    public boolean serverGround, lastServerGround, canFly, nearGround, worldLoaded, generalCancel, inVehicle, creative,
            sneaking, lsneaking, sprinting, gliding, riptiding, wasOnSlime, onLadder, doingVelocity, breakingBlock,
               inventoryOpen, swimming;
    public List<TrackedEntity> nearbyEntities = Collections.emptyList();
    public PastLocation targetPastLocation = new PastLocation();
    public KLocation lastKnownGoodPosition;
    public long lastArmSwing;
    public RollingAverage cps = new RollingAverage(10);
    public List<Vector3d> velocityHistory = Collections.synchronizedList(new EvictingList<>(5));
    public List<PlayerCapabilities> possibleCapabilities = new ArrayList<>();
    private int clientGroundTicks, clientAirTicks;
    private double walkSpeed, flySpeed;
    private PlayerInput playerInput;
    private Pose pose;
}
