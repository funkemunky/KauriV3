package dev.brighten.ac.check.impl.velocity;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.check.impl.speed.Horizontal;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import dev.brighten.ac.utils.Tuple;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CheckData(name = "Velocity (B)", type = CheckType.MOVEMENT)
public class VelocityB extends Check {
    public VelocityB(APlayer player) {
        super(player);

        player.onVelocity(velocity -> {
            pvX = velocity.getX();
            pvZ = velocity.getZ();
            ticks = 0;
            debug("did velocity: %.3f, %.3f", pvX, pvZ);
        });
    }

    private double pvX, pvZ;
    private boolean useEntity, sprint;
    private double buffer;
    private float fromFriction;
    private int ticks;
    private static final double[] moveValues = new double[] {-0.98, 0, 0.98};

    @Action
    public void onUseEntity(WPacketPlayInUseEntity packet) {
        if(!useEntity
                && packet.getAction().equals(WPacketPlayInUseEntity.EnumEntityUseAction.ATTACK)) {
            useEntity = true;
        }
    }

    @Action
    public void onFlying(WPacketPlayInFlying packet) {
        check: {
            if((pvX != 0 || pvZ != 0) && (player.getMovement().getDeltaX() != 0
                    || player.getMovement().getDeltaY() != 0
                    || player.getMovement().getDeltaZ() != 0)) {
                boolean found = false;

                double drag = 0.91;

                if(player.getBlockInfo().blocksNear
                        || player.getBlockInfo().blocksAbove
                        || player.getBlockInfo().inLiquid
                        || player.getLagInfo().getLastPacketDrop().isNotPassed(2)
                        || player.getLagInfo().getLastPingDrop().isNotPassed(4)) {
                    pvX = pvZ = 0;
                    buffer-= buffer > 0 ? 1 : 0;
                    break check;
                }

                if(player.getMovement().getFrom().isOnGround()) {
                    drag*= fromFriction;
                }

                if(useEntity && (sprint || (player.getBukkitPlayer().getItemInHand() != null
                        && player.getBukkitPlayer().getItemInHand().containsEnchantment(Enchantment.KNOCKBACK)))) {
                    pvX*= 0.6;
                    pvZ*= 0.6;
                }

                double f = 0.16277136 / (drag * drag * drag);
                double f5;

                if (player.getMovement().getFrom().isOnGround()) {
                    double aiMoveSpeed = (double) player.getBukkitPlayer().getWalkSpeed() / 2f;

                    if(player.getInfo().isSprinting()) aiMoveSpeed += aiMoveSpeed * 0.30000001192092896D;

                    aiMoveSpeed += (player.getPotionHandler()
                            .getEffectByType(PotionEffectType.SPEED)
                            .map(p -> p.getAmplifier() + 1).orElse(0))
                            * 0.20000000298023224D * aiMoveSpeed;
                    aiMoveSpeed += (player.getPotionHandler()
                            .getEffectByType(PotionEffectType.SLOW)
                            .map(p -> p.getAmplifier() + 1).orElse(0))
                            * -0.15000000596046448D * aiMoveSpeed;

                    f5 = aiMoveSpeed * f;
                } else {
                    f5 = sprint ? 0.026f : 0.02f;
                }

                double vX = pvX;
                double vZ = pvZ;

                List<Tuple<Double[], Double[]>> predictions = new ArrayList<>();

                double moveStrafe = 0, moveForward = 0;
                for (double forward : moveValues) {
                    for(double strafe : moveValues) {
                        moveFlying(strafe, forward, f5);

                        predictions.add(new Tuple<>(new Double[]{forward, strafe}, new Double[]{pvX, pvZ}));

                        pvX = vX;
                        pvZ = vZ;
                    }
                }

                Optional<Tuple<Double[],Double[]>> velocity = predictions.stream()
                        .filter(tuple -> {
                            double deltaX = Math.abs(tuple.two[0] - player.getMovement().getDeltaX());
                            double deltaZ = Math.abs(tuple.two[1] - player.getMovement().getDeltaZ());

                            return (deltaX * deltaX + deltaZ * deltaZ) < 0.005;
                        })
                        .min(Comparator.comparing(tuple -> {
                            double deltaX = Math.abs(tuple.two[0] - player.getMovement().getDeltaX());
                            double deltaZ = Math.abs(tuple.two[1] - player.getMovement().getDeltaZ());

                            return (deltaX * deltaX + deltaZ * deltaZ);
                        }));

                found = true;
                if(!velocity.isPresent()) {
                    Horizontal speedCheck = (Horizontal) player.findCheck(Horizontal.class);
                    double s2 = speedCheck.strafe;
                    double f2 = speedCheck.forward;

                    moveStrafe = s2;
                    moveForward = f2;

                    moveFlying(s2, f2, f5);
                } else {
                    Tuple<Double[], Double[]> tuple = velocity.get();

                    moveForward = tuple.one[0];
                    moveStrafe = tuple.one[1];
                    pvX = tuple.two[0];
                    pvZ = tuple.two[1];
                }

                double pvXZ = Math.hypot(pvX, pvZ);

                if(pvXZ < 0.2) break check;
                double ratio = player.getMovement().getDeltaXZ() / pvXZ;

                if((ratio < 0.996) && pvX != 0
                        && pvZ != 0
                        && player.getCreation().isPassed(3000L)
                        && player.getMovement().getLastTeleport().isPassed(1)
                        && !player.getBlockInfo().blocksNear) {
                    if(player.getInfo().lastUseItem.isPassed(2) && ++buffer > 11) {
                        flag("pct=%.2f buffer=%.1f forward=%.2f strafe=%.2f",
                                ratio * 100, buffer, moveStrafe, moveForward);
                        buffer = 11;
                    }
                } else if(buffer > 0) buffer-= 0.5;

                debug("ratio=%.3f dxz=%.4f vxz=%.4f vxz=%.4g,%.4f buffer=%.1f ticks=%s strafe=%.2f forward=%.2f " +
                                "found=%s lastV=%s", ratio, player.getMovement().getDeltaXZ(), pvXZ, pvX, pvZ,
                        buffer, ticks, moveStrafe, moveForward,
                        found, player.getInfo().getVelocity().getPassed());

                pvX = 0;
                pvZ = 0;
            }
        }
        sprint = player.getInfo().isSprinting();
        useEntity = false;
        fromFriction = player.getInfo().getBlockBelow()
                .map(b -> CraftMagicNumbers.getBlock(b.getType()).frictionFactor).orElse(0.6f);
    }

    private void moveFlying(double strafe, double forward, double friction) {
        double f = strafe * strafe + forward * forward;

        if (f >= 1.0E-4F) {
            f = Math.sqrt(f);

            if (f < 1.0F) {
                f = 1.0F;
            }

            f = friction / f;
            strafe = strafe * f;
            forward = forward * f;
            double f1 = Math.sin(player.getMovement().getTo().getLoc().yaw * Math.PI / 180.0F);
            double f2 = Math.cos(player.getMovement().getTo().getLoc().yaw * Math.PI / 180.0F);
            pvX += (strafe * f2 - forward * f1);
            pvZ += (forward * f2 + strafe * f1);
        }
    }
}
