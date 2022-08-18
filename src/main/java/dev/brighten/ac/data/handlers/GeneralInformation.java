package dev.brighten.ac.data.handlers;

import dev.brighten.ac.packet.wrapper.objects.PlayerCapabilities;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.PastLocation;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GeneralInformation {
    private Optional<Block> blockOnTo, blockBelow;
    private Timer lastMove = new TickTimer(), vehicleSwitch = new TickTimer(), lastAbilities = new TickTimer(),
            lastSneak = new TickTimer(), velocity = new TickTimer(), lastCancel = new TickTimer(),
            lastElytra = new TickTimer(), blockAbove = new TickTimer(), lastPlace = new TickTimer();
    private LivingEntity target;
    private boolean serverGround, lastServerGround, canFly, nearGround, worldLoaded, generalCancel, inVehicle, creative,
            sneaking, sprinting, gliding, riptiding, wasOnSlime, onLadder, doingVelocity;
    private List<Entity> nearbyEntities = Collections.emptyList();
    private PastLocation targetPastLocation = new PastLocation();
    private KLocation lastKnownGoodPosition;
    private List<Vector> velocityHistory = Collections.synchronizedList(new EvictingList<>(5));
    private List<PlayerCapabilities> possibleCapabilities = new ArrayList<>();
    private int clientGroundTicks, clientAirTicks;
}
