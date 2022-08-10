package dev.brighten.ac.data.handlers;

import dev.brighten.ac.utils.PastLocation;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GeneralInformation {
    private Optional<Block> blockOnTo, blockBelow;
    private Timer lastMove = new TickTimer(), vehicleSwitch = new TickTimer();
    private LivingEntity target;
    private boolean serverGround, lastServerGround, nearGround, worldLoaded, generalCancel, inVehicle, creative,
            sneaking, sprinting;
    private List<Entity> nearbyEntities = Collections.emptyList();
    private PastLocation targetPastLocation = new PastLocation();;
}
