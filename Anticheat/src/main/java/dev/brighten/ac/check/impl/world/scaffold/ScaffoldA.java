package dev.brighten.ac.check.impl.world.scaffold;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInEntityAction;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInHeldItemSlot;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.objects.evicting.EvictingList;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

@CheckData(name = "Scaffold", checkId = "scaffold", type = CheckType.INTERACT, maxVersion = ProtocolVersion.V1_21_4)
public class ScaffoldA extends Check {
    public ScaffoldA(APlayer player) {
        super(player);
    }

    private final Collection<BlockFace> FACES = EnumSet.of(
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH);

    private final double MAX_ANGLE = Math.toRadians(90);

    private int lastSlot, slotChangeCount, lastBlockY, lastPlaceTick, lastLocY;
    private float lastYaw;
    private long sprintTick, sneakTick, lastSlotTick, lastLookTick, lastValidPlace,
            setSwitchPairTick, blockPlaceTick, slotFreqStart, lastFlagTick;

    private int angleVl, sprintVl, rotateVl, timeVl, slotFreqVl, towerVl;
    private float switchSlotVl;
    private Block lastBlockPlaced;

    private Tuple<Integer, Integer> switchPair;
    private final List<Long> placeDiff = new EvictingList<>(8);
    private final HashSet<Integer> slotChanges = new HashSet<>();


    @Bind
    public WAction<BlockPlaceEvent> blockplace = event -> {
        Location location = event.getPlayer().getLocation();
        final Block blockPlaced = event.getBlockPlaced();
        final BlockFace blockAgainst = event.getBlock().getFace(event.getBlockAgainst());

        final int serverTick = Anticheat.INSTANCE.getCurrentTick();
        int playerTick = player.getPlayerTick();

        // If we are unable to use transaction ticks for the player, instead using the server tick for timing

        final boolean valid = location.getY() - blockPlaced.getY() < 2 && location.getY() - blockPlaced.getY() >= 1D
                && location.distance(blockPlaced.getLocation()) < 2;


        final boolean exists = FACES.contains(blockAgainst);

        if ((lastBlockY + 1 == event.getBlock().getY())
                && location.getY() - 1 == blockPlaced.getY()
                && (playerTick - lastPlaceTick < 6)
                && (((int) location.getY()) > lastLocY)
                && valid) {
            if (towerVl++ > 1) {
                flag("flag=TOWER vl=" + towerVl + " block=" + blockPlaced.getType().name() + " against=" + blockAgainst.name());
            }
        }


        // Flag if player rotated quickly just before placing a block
        if ((playerTick - lastFlagTick <= 2)) {

            float deltaYaw = MathUtils.yawTo180F(player.getMovement().getFrom().getYaw()) - MathUtils.yawTo180F(player.getMovement().getTo().getYaw());
            double deltaXZ = player.getMovement().getDeltaXZ();
            double lastDeltaXZ = player.getMovement().getLDeltaXZ();
            if ((deltaXZ > 0.18 && (Math.abs(deltaXZ - lastDeltaXZ) < 0.000144 || deltaXZ > lastDeltaXZ)) && deltaYaw > 50f) {
                flag("flag=ROTATE deltaYaw=%.2f deltaXZ=%.3f lDeltaXZ=%.3f", deltaYaw, player.getMovement().getDeltaXZ(), player.getMovement().getLDeltaXZ());
            }
        }

        // Multiple conditions checked to make sure we are only checking scaffold behavior
        // This will only check the side block faces, not the top or bottom. The block placed must be right underneath the player
        if (exists && valid) {
            final BlockFace placedFace = event.getBlock().getFace(event.getBlockAgainst());

            // Angle check
            final Vector placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());
            float placedAngle = player.getBukkitPlayer().getLocation().getDirection().angle(placedVector);

            // Sprint check
            final boolean ground = player.getBukkitPlayer().getLocation().getY() % 0.015625 == 0;

            // Ensure that the player is looking at least within 90 degrees of the blockFace vector
            if (placedAngle > MAX_ANGLE) {
                if (angleVl++ > 2) {
                    flag("flag=ANGLE angle=%.3f lookTick=%d placeTick=%d tick=%d/%d", placedAngle, (playerTick - lastLookTick), (playerTick - blockPlaceTick), playerTick, serverTick);
                    if(angleVl > 7) {
                        angleVl = 0;
                    }
                }
            }
            debug("YAW=%.1f", player.getBukkitPlayer().getLocation().getYaw());

            //debug(player, k -> String.format("pitch=%f onGround=%b sprintDiff=%o placeDiff=%o", player.getLocation().getPitch(), ground, (finalPlayerTick - sprintTick), finalPlayerTick - blockPlaceTick));
            if (player.getBukkitPlayer().getLocation().getPitch() > 75) {

                final boolean isSameLocation = lastBlockPlaced != null
                        && MovementUtils.isSameLocation(event.getBlockAgainst().getLocation(), lastBlockPlaced.getLocation());

                /*
                 * Sprint check - prevent cheaters from sprinting while scaffolding
                 * Conditions checked: Player is looking down, onGround,
                 * sprinting for 8 ticks, placing against the last block placed, and within a second of last block placed
                 * Allow NCP/Speed checks to deal with fake sprinting
                 */

                final boolean ticked = sprintTick + 8 < playerTick
                        && playerTick - blockPlaceTick < 6;
                final boolean environment = player.getInfo().isSprinting() && ground && player.getMovement().getDeltaY() >= 0.21;

                if (ticked && environment && isSameLocation) {
                    if (++sprintVl > 3) {
                        flag("flag=SPRINT sprint=%d p=%.2f deltaXZ=%.3f lastDeltaXZ=%.3f",
                                (playerTick - sprintTick),
                                player.getBukkitPlayer().getLocation().getPitch(),
                                player.getMovement().getDeltaXZ(),
                                player.getMovement().getLDeltaXZ());

                        if(sprintVl > 11) {
                            sprintVl = 0;
                        }
                    }

                }

                /*
                 * Time check
                 * Basically a fastplace check but for scaffold placements. Ensure the player is placing blocks below them
                 * in a reasonable time between each block placement. This should be a fail-all case if the player is not
                 * detected by any other detections. This should at least set some long term protection from scaffold
                 * and prevent the player from gaining any major advantages from using scaffold.
                 */
                timeCheck: {
                    final double deltaX = Math.abs(player.getMovement().getDeltaX());
                    final double deltaZ = Math.abs(player.getMovement().getDeltaZ());
                    final double deltaXZ = player.getMovement().getDeltaXZ();

                    final boolean sneaking = sneakTick + 10 > playerTick || player.getInfo().isSneaking();
                    final boolean smallMove = deltaX < 0.24 && deltaZ < 0.24;

                    // Skip if the player has recently stopped sneaking, currently sneaking, has speed, a deltaXZ below 0.19, or the block placed is not against the last block placed
                    if (!isSameLocation || sneaking || smallMove
                            || player.getPotionHandler().hasPotionEffect(PotionEffectType.SPEED)) {
                        //debug(player, k -> String.format("Time Exempt - sneaking=%b delta(%.3f, %.3f, %.3f) isSame=%b", sneaking, deltaX, deltaZ, deltaXZ, isSameLocation));
                        break timeCheck;
                    }
                    // Add first block placement time if not set, or set if the player's last block placement was more than 5 seconds ago
                    if (lastValidPlace == 0 || lastValidPlace + 60 < playerTick) {
                        break timeCheck;
                    }

                    // Add diff to placeDiff
                    debug("diff=%o", (playerTick - lastValidPlace));
                    placeDiff.add((playerTick - lastValidPlace));

                    final boolean sized = placeDiff.size() > 3;

                    if (sized) {
                        final long average = MathUtils.getAverageLong(placeDiff);
                        final boolean timed = average <= 6;

                        debug("average=%o", average);

                        if (timed) {
                            if (++timeVl > 2) {
                                flag("avg=%d difs=%s dxz=%.4f dx=%.4f dz=%.4f",
                                        average, placeDiff.toString(), deltaXZ, deltaX, deltaZ,
                                        player.getBukkitPlayer().getLocation().getPitch());
                                if(timeVl > 8) {
                                    punish();
                                    timeVl = 0;
                                }
                            }
                        } else {
                            placeDiff.clear();
                            lastValidPlace = 0;
                        }
                    }
                }
                lastValidPlace = playerTick;
            }
        }

        lastPlaceTick = playerTick;
        lastBlockY = blockPlaced.getY();
        blockPlaceTick = playerTick;
        lastBlockPlaced = event.getBlockPlaced();
        lastPlaceTick = playerTick;
        lastBlockY = blockPlaced.getY();
        lastLocY = (int) location.getY();
    };

    @Bind
    WAction<WPacketPlayInHeldItemSlot> heldItemSlotWAction = packet -> {
        final int serverTick = Anticheat.INSTANCE.getCurrentTick();
        int playerTick = player.getPlayerTick();

        boolean flag = false;
        final int slot = packet.getHandIndex();

        /*
         * Item Spoof checks
         *
         * These checks are designed at detecting a common
         * behavior in Scaffold's where the player is placing blocks
         * while it appears they are holding another item. This
         * is done by comparing common packet timing of held item packets
         * and block place packets.
         *
         */

        debug("placeTick=" + (playerTick - blockPlaceTick) + " slotTick=" + (playerTick - lastSlotTick));

        // Player quickly switched slots before and after placing a block
        // Checking if there was a slot change in the last tick, and a there was a block place sent for this current tick
        if ((playerTick - blockPlaceTick <= 2) && (playerTick - lastSlotTick <= 2)) {

            flag = true;

            // Every time the player fails the check, their current and previous slot
            // are saved and compared to the next time the player fails the check.
            // This is done as typically scaffolds will only be switching between two slots
            // and as a way to prevent any possible false positives
            if (switchPair != null
                    && switchPair.one == lastSlot
                    && switchPair.two == slot) {

                flag("flag=SWITCH slot=%d ls=%d bp=%d sc=%d pairtick=%d pd=%d tick=%d/%d", slot, lastSlot, (playerTick - blockPlaceTick), (playerTick - lastSlotTick),
                        (playerTick - setSwitchPairTick), player.getLagInfo().getLastPacketDrop().getPassed(), playerTick, serverTick);

                switchSlotVl++;

                if(switchSlotVl > 5) {
                    punish();
                    switchSlotVl = 0;
                }
            }

            switchPair = new Tuple<>(lastSlot, slot);
            setSwitchPairTick = playerTick;
        }

        // If SWITCH is unable to detect the cheaters item spoof technique
        // this check serves as a fallback detection. This will simply check if the
        // player is quickly switching their held item after placing blocks.
        if (playerTick - blockPlaceTick <= 5
                && playerTick - lastSlotTick <= 5) {

            slotChanges.add(slot);

            if (playerTick - slotFreqStart > 60) {
                debug("End of tracking, count=" + slotChangeCount
                        + " slots=" + slotChanges.size());
                // Check count
                if (slotChangeCount > 10
                        && slotChanges.size() < 4) {

                    // More likely the player is cheating, increment the VL more
                    if (slotChangeCount >= 17 && slotChanges.size() == 2) slotFreqVl++;

                    if(slotFreqVl++ > 3) {
                        slotFreqVl = 0;
                    }

                    flag("FLAG=SLOT_FREQ slots=%d tick=%d/%d", slotChangeCount, playerTick, serverTick);
                }

                slotChangeCount = 0;
                slotFreqStart = playerTick;
                slotChanges.clear();

            } else {
                slotChangeCount++;
            }

        }

        lastSlotTick = playerTick;
        lastSlot = slot;

        final boolean switched = playerTick - setSwitchPairTick > 1000;
        if (!flag && switched) {
            if(switchSlotVl > 2) {
                switchSlotVl *= 0.95f;
            }
        }
    };

    @Bind
    WAction<WPacketPlayInFlying> flying = packet -> {
        if(!packet.isLooked()) {
            return;
        }
        final int serverTick = Anticheat.INSTANCE.getCurrentTick();
        int playerTick = player.getPlayerTick();

        final float yaw = packet.getYaw();

        if (player.getMovement().getLastTeleport().isPassed(5)
                && lastLookTick != playerTick) {

            final float lastYaw = MathUtils.yawTo180F(this.lastYaw);
            final float currentYaw = MathUtils.yawTo180F(yaw);

            final float deltaYaw = MathUtils.getAngleDelta(lastYaw, currentYaw);
            final double lastDeltaXZ = player.getMovement().getLDeltaXZ();

            final double deltaXZ = player.getMovement().getDeltaXZ();
            final boolean large = deltaYaw > 70.f;
            final boolean speedup = deltaXZ > .19; //.21 but squared

            final boolean close = Math.abs(deltaXZ - lastDeltaXZ) < 0.000144; //0.012 squared
            final boolean accelerated = deltaXZ > lastDeltaXZ;

            final boolean violation = large && (close || accelerated) && speedup;

            // Detect quick rotations just before the player starts
            // placing blocks. No violation here unless the player places a block
            if ((playerTick - lastValidPlace > 60) && violation) {
                lastFlagTick = playerTick;
                return;
            }

            if (violation) {

                if (++rotateVl > 2) {

                    if(rotateVl > 6) {
                        punish();
                        rotateVl = 0;
                    }
                    flag("yaw=(%.3f, %.3f) dxz=(%.4f, %.4f) dyaw=%.3f tick=(%d, %d)",
                            currentYaw, lastYaw, deltaXZ, lastDeltaXZ,
                            deltaYaw, playerTick, serverTick);
                }
            }
        }

        lastLookTick = playerTick;
        lastYaw = yaw;
    };

    @Bind
    WAction<WPacketPlayInEntityAction> entityAction = packet -> {
        int playerTick = player.getPlayerTick();

        if (packet.getAction() == WPacketPlayInEntityAction.EnumPlayerAction.STOP_SNEAKING) {
            sneakTick = playerTick;
        } else if (packet.getAction() == WPacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING) {
            sprintTick = playerTick;
        }
    };
}
