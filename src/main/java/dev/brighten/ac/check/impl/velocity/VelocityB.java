package dev.brighten.ac.check.impl.velocity;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.check.impl.speed.Speed;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInUseEntity;
import dev.brighten.ac.utils.Tuple;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@CheckData(name = "Velocity (B)", type = CheckType.MOVEMENT)
public class VelocityB extends Check {
    public VelocityB(APlayer player) {
        super(player);

        player.onVelocity(velocity -> {
            pvX = velocity.getX();
            pvZ = velocity.getZ();
            ticks = 0;
            debug("did velocity");
        });
    }

    private double pvX, pvZ;
    private boolean useEntity, sprint;
    private double buffer;
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
        if((pvX != 0 || pvZ != 0) && (getPlayer().getMovement().getDeltaX() != 0
                || getPlayer().getMovement().getDeltaY() != 0
                || getPlayer().getMovement().getDeltaZ() != 0)) {
            boolean found = false;

            double drag = 0.91;

            if(getPlayer().getBlockInformation().blocksNear
                    || getPlayer().getBlockInformation().blocksAbove
                    || getPlayer().getBlockInformation().inLiquid
                    || getPlayer().getLagInfo().getLastPingDrop().isNotPassed()) {
                pvX = pvZ = 0;
                buffer-= buffer > 0 ? 1 : 0;
                return;
            }

            if(getPlayer().getMovement().getFrom().isOnGround()) {
                drag*= getPlayer().getBlockInformation().fromFriction;
            }

            if(useEntity && (sprint || (getPlayer().getBukkitPlayer().getItemInHand() != null
                    && getPlayer().getBukkitPlayer().getItemInHand().containsEnchantment(Enchantment.KNOCKBACK)))) {
                pvX*= 0.6;
                pvZ*= 0.6;
            }

            double f = 0.16277136 / (drag * drag * drag);
            double f5;

            if (getPlayer().getMovement().getFrom().isOnGround()) {
                AtomicReference<Double> aiMoveSpeed = new AtomicReference<>((double) getPlayer().getBukkitPlayer().getWalkSpeed() / 2f);

                if(getPlayer().getInfo().isSprinting()) aiMoveSpeed.updateAndGet(v -> new Double((double) (v + aiMoveSpeed.get() * 0.3f)));

                getPlayer().getPotionHandler().getEffectByType(PotionEffectType.SPEED).ifPresent(speed ->
                        aiMoveSpeed.updateAndGet(v -> new Double((double) (v + (speed.getAmplifier() + 1) * (double) 0.2f * aiMoveSpeed.get()))));
                getPlayer().getPotionHandler().getEffectByType(PotionEffectType.SLOW).ifPresent(speed ->
                        aiMoveSpeed.updateAndGet(v -> new Double((double) (v + (speed.getAmplifier() + 1) * (double) -0.15f * aiMoveSpeed.get()))));
                f5 = aiMoveSpeed.get() * f;
            } else {
                f5 = sprint ? 0.026f : 0.02f;
            }

            double vX = pvX;
            double vZ = pvZ;
            double vXZ = 0;

            List<Tuple<Double[], Double[]>> predictions = new ArrayList<>();

            double moveStrafe = 0, moveForward = 0;
            for (double forward : moveValues) {
                for(double strafe : moveValues) {
                    double s2 = strafe;
                    double f2 = forward;

                    moveFlying(s2, f2, f5);

                    predictions.add(new Tuple<>(new Double[]{f2, s2}, new Double[]{pvX, pvZ}));

                    pvX = vX;
                    pvZ = vZ;
                }
            }

            Optional<Tuple<Double[],Double[]>> velocity = predictions.stream()
                    .filter(tuple -> {
                        double deltaX = Math.abs(tuple.two[0] - getPlayer().getMovement().getDeltaX());
                        double deltaZ = Math.abs(tuple.two[1] - getPlayer().getMovement().getDeltaZ());

                        return (deltaX * deltaX + deltaZ * deltaZ) < 0.005;
                    })
                    .min(Comparator.comparing(tuple -> {
                        double deltaX = Math.abs(tuple.two[0] - getPlayer().getMovement().getDeltaX());
                        double deltaZ = Math.abs(tuple.two[1] - getPlayer().getMovement().getDeltaZ());

                        return (deltaX * deltaX + deltaZ * deltaZ);
                    }));

            found = true;
            if(!velocity.isPresent()) {
                Speed speedCheck = (Speed) getPlayer().findCheck(Speed.class);
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

            double pvXZ = Math.sqrt(pvX * pvX + pvZ * pvZ);
            double ratio = getPlayer().getMovement().getDeltaXZ() / pvXZ;

            if((ratio < 0.996) && pvX != 0
                    && pvZ != 0
                    && getPlayer().getCreation().isPassed(3000L)
                    && getPlayer().getMovement().getLastTeleport().isPassed(1)
                    && !getPlayer().getBlockInformation().blocksNear) {
                if(++buffer > 30) {
                    flag("pct=%.2f buffer=%.1f forward=%.2f strafe=%.2f",
                            ratio * 100, buffer, moveStrafe, moveForward);
                    buffer = 31;
                }
            } else if(buffer > 0) buffer-= 0.5;

            debug("ratio=%.3f dx=%.4f dz=%.4f buffer=%.1f ticks=%s strafe=%.2f forward=%.2f " +
                            "found=%s lastV=%s", ratio, getPlayer().getMovement().getDeltaX(), getPlayer().getMovement().getDeltaZ(),
                    buffer, ticks, moveStrafe, moveForward,
                    found, getPlayer().getInfo().getVelocity().getPassed());

            pvX *= drag;
            pvZ *= drag;

            if(++ticks > 6) {
                ticks = 0;
                pvX = pvZ = 0;
            }

            if(Math.abs(pvX) < 0.005) pvX = 0;
            if(Math.abs(pvZ) < 0.005) pvZ = 0;
        }
        sprint = getPlayer().getInfo().isSprinting();
        useEntity = false;
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
            double f1 = Math.sin(getPlayer().getMovement().getTo().getLoc().yaw * Math.PI / 180.0F);
            double f2 = Math.cos(getPlayer().getMovement().getTo().getLoc().yaw * Math.PI / 180.0F);
            pvX += (strafe * f2 - forward * f1);
            pvZ += (forward * f2 + strafe * f1);
        }
    }
}
