package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.wrapper.Wrapper;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@CheckData(name = "Speed", checkId = "speed", type = CheckType.MOVEMENT)
public class Speed extends Check {

    public Speed(APlayer player) {
        super(player);
    }
    private float buffer;
    private boolean previousFromGround;

    WAction<WPacketPlayInFlying> flyingPacket = packet -> {
        if(player.getMovement().isExcuseNextFlying()) return;
        checkProccesing:
        {
            if (!packet.isMoved())
                break checkProccesing;

            IntVector belowLoc = new IntVector(MathHelper.floor_double(player.getMovement().getFrom().getX()),
                    MathHelper.floor_double(player.getMovement().getFrom().getY() - 1),
                    MathHelper.floor_double(player.getMovement().getFrom().getZ()));
            List<Float> frictions = player.getBlockUpdateHandler().getPossibleMaterials(belowLoc).stream()
                    .map(Wrapper.getInstance()::getFriction).collect(Collectors.toList());

            Tuple<Double, TagsBuilder> ratio = frictions.stream().map(friction -> {
                float drag = 0.91f;
                double ldxz = player.getMovement().getDeltaXZ();

                if(previousFromGround) {
                    drag*= friction;
                }

                TagsBuilder tags = new TagsBuilder();
                double moveFactor = player.getBukkitPlayer().getWalkSpeed() / 2f;

                moveFactor+= moveFactor * 0.3f;

                if(player.getPotionHandler().hasPotionEffect(PotionEffectType.SPEED))
                    moveFactor += (PlayerUtils.getPotionEffectLevel(player.getBukkitPlayer(), PotionEffectType.SPEED)
                            * (0.20000000298023224D)) * moveFactor;

                if(player.getPotionHandler().hasPotionEffect(PotionEffectType.SLOW))
                    moveFactor += (PlayerUtils.getPotionEffectLevel(player.getBukkitPlayer(), PotionEffectType.SLOW)
                            * (-0.15000000596046448D)) * moveFactor;

                if (player.getMovement().getFrom().isOnGround()) {
                    tags.addTag("ground");
                    drag *= 0.91f;
                    moveFactor *= 0.16277136 / (drag * drag * drag);

                    if (player.getMovement().isJumped()) {
                        tags.addTag("jumped");
                        moveFactor += 0.2;
                    }
                } else {
                    tags.addTag("air");
                    drag = 0.91f;
                    moveFactor = 0.026f;
                }

                if(player.getBlockInfo().inWater) {
                    tags.addTag("water");

                    drag = player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_13) ? 0.9f : 0.8f;
                    moveFactor = 0.034;

                    if(player.getInfo().lastLiquid.getResetStreak() < 3) {
                        tags.addTag("water-enter");
                        moveFactor*= 1.35;
                    }
                } else if(player.getInfo().lastLiquid.isNotPassed(3)) {
                    moveFactor*= 1.35;
                    tags.addTag("water-leave");
                }

                if(player.getMovement().getLastTeleport().isNotPassed(6)
                        || player.getInfo().getLastRespawn().isNotPassed(6)) {
                    tags.addTag("teleport");
                    moveFactor+= 0.1;
                    moveFactor*= 5;
                }

                //In 1.9+, entity collisions add acceleration to their movement.
                if(player.getInfo().lastEntityCollision.isNotPassed(2)) {
                    tags.addTag("entity-collision");
                    moveFactor+= 0.05;
                }

                //Pistons have the ability to move players 1 whole block
                if(player.getBlockInfo().pistonNear) {
                    tags.addTag("piston");
                    moveFactor+= 1;
                }

                if(player.getBlockInfo().onSlime && player.getMovement().getFrom().isOnGround()) {
                    if (Math.abs(player.getMovement().getDeltaY()) < 0.1D && !player.getInfo().isSneaking()) {
                        moveFactor*= 0.5;
                    }
                }

                if(player.getBlockInfo().inWeb
                        //Ensuring they aren't just entering or leaving web
                        && player.getInfo().getLastWeb().getResetStreak() > 1) {
                    tags.addTag("web");
                    moveFactor*= 0.4;
                }

                if(player.getBlockInfo().onSoulSand && player.getMovement().getFrom().isOnGround()
                        //Ensuring the player is actually standing on the block and recieving slow
                        && packet.getY() % (1) == 0.875) {
                    tags.addTag("soulsand");
                    moveFactor*= 0.88;
                }

               return new Tuple<>((player.getMovement().getDeltaXZ() - ldxz) / moveFactor * 100D, tags);
            }).min(Comparator.comparing(r -> MathUtils.getDelta(r.one, 100))).orElse(new Tuple<>(100D, new TagsBuilder()));

            if (ratio.one > 100.8
                    && !player.getBlockInfo().inHoney
                    && !player.getBlockInfo().inScaffolding
                    && player.getInfo().velocity.isPassed(2)
                    && player.getInfo().lastLiquid.isPassed(2)
                    && !player.getInfo().generalCancel) {
                if(++buffer > 2) {
                    vl++;
                    flag("p=%.1f%% dxz=%.3f a/g=%s,%s tags=%s",
                            ratio.one, player.getMovement().getDeltaXZ(), player.getMovement().getAirTicks(), player.getMovement().getGroundTicks(),
                           ratio.two.build());
                    buffer = Math.min(5, buffer); //Preventing runaway flagging
                }
            } else if(buffer > 0) buffer-= 0.2f;
            debug("ratio=%.1f deltaY=%.3f tags=%s tp=%s buffer=%.1f", ratio.one, player.getMovement().getDeltaY(),
                    ratio.two.build(), player.getInfo().getLastLiquid().getPassed(), buffer);
        }
        previousFromGround = player.getMovement().getFrom().isOnGround();
    };
}
