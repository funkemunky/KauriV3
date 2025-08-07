package dev.brighten.ac.check.impl.packet.badpackets;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.handler.events.ServerPositionEvent;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.annotation.Bind;

import java.util.*;

@CheckData(name = "Teleport", checkId = "teleport", type = CheckType.BADPACKETS, description = "Detects clients trying to bypass teleport restrictions.")
public class Teleport extends Check {

    public Teleport(APlayer player) {
        super(player);
    }

    private final Map<ServerPositionEvent, Boolean> positions = new HashMap<>();

    private KLocation originalFromLocation;
    private int teleportTick;
    private double movementDistance;

    @Bind
    WAction<ServerPositionEvent> serverPosition = event -> {
        positions.put(event, false);

        if(player.getPlayerVersion().isOlderThan(ClientVersion.V_1_9)) {
            player.runKeepaliveAction(ka -> positions.remove(event), 1);
        }
    };

    @Bind
    WAction<WrapperPlayClientTeleportConfirm> teleportConfirm = packet -> {
        if(positions.isEmpty()) {
            flag("Teleport confirm without any positions recorded.");
            return;
        }

        Optional<ServerPositionEvent> event = positions.keySet().stream()
                .filter(pos -> pos.id() == packet.getTeleportId())
                .findAny();

        if(event.isEmpty()) {
            flag("Teleport confirm with no matching position event for ID: " + packet.getTeleportId());
            return;
        }

        if(!positions.get(event.get())) {
            flag("Teleport confirm for position ID: " + packet.getTeleportId() + " but never received a position update.");
        }

        positions.remove(event.get());
    };

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {
        if(!packet.hasPositionChanged()) {
            return;
        }

        teleportHandling: {
            if(packet.isOnGround()) {
                // If the player is on the ground, we can assume they are not teleporting.
                break teleportHandling;
            }

            if(positions.isEmpty()) {
                break teleportHandling;
            }

            KLocation location = new KLocation(packet.getLocation().getX(), packet.getLocation().getY(),
                    packet.getLocation().getZ(), packet.getLocation().getYaw(),
                    packet.getLocation().getPitch());

            // Check if the player has sent a position update before flying.
            Optional<ServerPositionEvent> lastPosition = positions.keySet().stream()
                    .min(Comparator.comparingDouble(pos -> location.distanceSquared(pos.getKLocation())));

            if(lastPosition.isEmpty()) {
                break teleportHandling;
            }

            if(lastPosition.get().getKLocation().distance(lastPosition.get().getKLocation()) < 1E-10) {
                positions.put(lastPosition.get(), true);
                movementDistance = lastPosition.get().getKLocation().distance(player.getMovement().getFrom().getLoc());
                originalFromLocation = player.getMovement().getFrom().getLoc();
                teleportTick = player.getPlayerTick();
            }
        }

        if(originalFromLocation == null) {
            return;
        }

        if(player.getPlayerTick() - teleportTick > 5) {
            // If more than 10 ticks have passed since the teleport, reset the original location.
            originalFromLocation = null;
            teleportTick = 0;
            movementDistance = 0;
            return;
        }

        if(originalFromLocation.distanceSquared(player.getMovement().getTo().getLoc()) < 1E-4 && movementDistance > 0.1) {
            flag("Teleport abuse");
        }
    };
}
