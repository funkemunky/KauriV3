package dev.brighten.ac.check.impl.movement.velocity;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathHelper;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.potion.PotionEffectType;

import java.util.Deque;

@CheckData(name = "Velocity (B)", checkId = "velocityb", type = CheckType.MOVEMENT)
public class VelocityB extends Check {
    private Timer lastVelocity = new TickTimer();

    public VelocityB(APlayer player) {
        super(player);

        player.onVelocity(velocity -> {
            if (lastVelocity.isPassed(10)) { //Temp fix for combo mode
                pvX = velocity.getX();
                pvZ = velocity.getZ();
                ticks = 1;
                debug("did velocity: %.3f, %.3f", pvX, pvZ);
            }

            lastVelocity.reset();
        });
    }

    private double pvX, pvZ;
    private boolean useEntity, sprint;
    private int ticks;
    private double buffer;
    private float friction;
    private KLocation previousFrom;
    private static final boolean[] TRUE_FALSE = new boolean[]{true, false};

    public double strafe, forward;

    WAction<WPacketPlayInFlying> flying = packet -> {

        check:
        {
            if (ticks == 0) break check;
            Block underBlock = BlockUtils.getBlock((previousFrom != null ? player.getMovement().getFrom().getLoc() : player.getMovement().getTo().getLoc())
                    .toLocation(player.getBukkitPlayer().getWorld())
                    .subtract(0, 1, 0)),
                    lastUnderBlock = BlockUtils.getBlock((previousFrom != null ? previousFrom : player.getMovement().getFrom().getLoc())
                            .toLocation(player.getBukkitPlayer().getWorld())
                            .subtract(0, 1, 0));
            if (underBlock == null || lastUnderBlock == null)
                break check;

            Deque<Material> frictionList = player.getBlockUpdateHandler()
                    .getPossibleMaterials(new IntVector(underBlock.getX(), underBlock.getY(), underBlock.getZ())),
                    lfrictionList = player.getBlockUpdateHandler()
                            .getPossibleMaterials(new IntVector(lastUnderBlock.getX(), lastUnderBlock.getY(), lastUnderBlock.getZ()));

            if (player.getMovement().getMoveTicks() == 0
                    || player.getInfo().isGeneralCancel()
                    || player.getBlockInfo().onClimbable
                    || player.getInfo().lastWeb.isNotPassed(2)
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getBlockInfo().collidesHorizontally) {
                ticks = 0;
                break check;
            }
            double smallestDelta = Double.MAX_VALUE;

            double pmotionx = 0, pmotionz = 0;
            boolean onGround = player.getMovement().getFrom().isOnGround();

            loop:
            {
                for (int f = -1; f < 2; f++) {
                    for (Material underMaterial : frictionList) {
                        for (int s = -1; s < 2; s++) {
                            for (boolean sprinting : TRUE_FALSE) {
                                for (int fastMath = 0; fastMath <= 2; fastMath++) {
                                    for (boolean attack : TRUE_FALSE) {
                                        for (boolean using : TRUE_FALSE) {
                                            for (boolean sneaking : TRUE_FALSE) {
                                                for (boolean jumped : TRUE_FALSE) {

                                                    float forward = f, strafe = s;

                                                    if (sprinting && forward <= 0) {
                                                        continue;
                                                    }

                                                    if (sneaking) {
                                                        forward *= 0.3;
                                                        strafe *= 0.3;
                                                    }

                                                    float friction = CraftMagicNumbers.getBlock(underMaterial).frictionFactor;

                                                    if (using) {
                                                        forward *= 0.2;
                                                        strafe *= 0.2;
                                                    }

                                                    //Multiplying by 0.98 like in client
                                                    forward *= 0.9800000190734863F;
                                                    strafe *= 0.9800000190734863F;

                                                    double aiMoveSpeed = player.getBukkitPlayer().getWalkSpeed() / 2;

                                                    float drag = 0.91f;
                                                    double lmotionX = pvX,
                                                            lmotionZ = pvZ;

                                                    //lmotionX *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;
                                                    //lmotionZ *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;

                                                    //Running multiplication done after previous prediction
                                                    if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                                                        if (Math.abs(lmotionX) < 0.003)
                                                            lmotionX = 0;
                                                        if (Math.abs(lmotionZ) < 0.003)
                                                            lmotionZ = 0;
                                                    } else {
                                                        if (Math.abs(lmotionX) < 0.005)
                                                            lmotionX = 0;
                                                        if (Math.abs(lmotionZ) < 0.005)
                                                            lmotionZ = 0;
                                                    }

                                                    //Less than 0.05
                                                    if (((lmotionX * lmotionX) + (lmotionZ * lmotionZ)) < 0.0025 && player.getMovement().getDeltaXZ() < 0.2) {
                                                        break check;
                                                    }
                                                    // Attack slowdown
                                                    if (attack) {
                                                        lmotionX *= 0.6;
                                                        lmotionZ *= 0.6;
                                                    }

                                                    if (sprinting)
                                                        aiMoveSpeed += aiMoveSpeed * 0.30000001192092896D;

                                                    if (player.getPotionHandler().hasPotionEffect(PotionEffectType.SPEED))
                                                        aiMoveSpeed += (player.getPotionHandler().getEffectByType(PotionEffectType.SPEED)
                                                                .get()
                                                                .getAmplifier() + 1) * (double) 0.20000000298023224D * aiMoveSpeed;
                                                    if (player.getPotionHandler().hasPotionEffect(PotionEffectType.SLOW))
                                                        aiMoveSpeed += (player.getPotionHandler().getEffectByType(PotionEffectType.SLOW)
                                                                .get()
                                                                .getAmplifier() + 1) * (double) -0.15000000596046448D * aiMoveSpeed;

                                                    float f5;
                                                    if (onGround) {
                                                        drag *= friction;

                                                        f5 = (float) (aiMoveSpeed * (0.16277136F / (drag * drag * drag)));

                                                        if (sprinting && jumped) {
                                                            float rot = player.getMovement().getTo().getLoc().yaw * 0.017453292F;
                                                            lmotionX -= sin(fastMath, rot) * 0.2F;
                                                            lmotionZ += cos(fastMath, rot) * 0.2F;
                                                        }

                                                    } else f5 = sprinting ? 0.025999999F : 0.02f;

                                                    if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                                                        double keyedMotion = forward * forward + strafe * strafe;

                                                        if (keyedMotion >= 1.0E-4F) {
                                                            keyedMotion = f5 / Math.max(1.0, Math.sqrt(keyedMotion));
                                                            forward *= keyedMotion;
                                                            strafe *= keyedMotion;

                                                            final float yawSin = sin(fastMath,
                                                                    player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                                                    yawCos = cos(fastMath,
                                                                            player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F);

                                                            lmotionX += (strafe * yawCos - forward * yawSin);
                                                            lmotionZ += (forward * yawCos + strafe * yawSin);
                                                        }
                                                    } else {
                                                        float keyedMotion = forward * forward + strafe * strafe;

                                                        if (keyedMotion >= 1.0E-4F) {
                                                            keyedMotion = f5 / Math.max(1.0f, MathHelper.sqrt_float(keyedMotion));
                                                            forward *= keyedMotion;
                                                            strafe *= keyedMotion;

                                                            final float yawSin = sin(fastMath,
                                                                    player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                                                    yawCos = cos(fastMath,
                                                                            player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F);

                                                            lmotionX += (strafe * yawCos - forward * yawSin);
                                                            lmotionZ += (forward * yawCos + strafe * yawSin);
                                                        }
                                                    }
                                                    double diffX = player.getMovement().getDeltaX() - lmotionX,
                                                            diffZ = player.getMovement().getDeltaZ() - lmotionZ;
                                                    double delta = (diffX * diffX) + (diffZ * diffZ);

                                                    if (delta < smallestDelta) {
                                                        smallestDelta = delta;
                                                        pmotionx = lmotionX;
                                                        pmotionz = lmotionZ;
                                                        this.friction = friction;

                                                        if (delta < 4E-17) {
                                                            this.strafe = s * 0.98f;
                                                            this.forward = f * 0.98f;

                                                            break loop;
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

            if (++ticks > 6) {
                ticks = 0;
            }

            double pmotion = Math.hypot(pmotionx, pmotionz);

            double ratio = Math.abs(player.getMovement().getDeltaXZ() / pmotion);

            if (ratio < 0.992) {
                if (++buffer > 10) {
                    flag("p=%.1f%%", ratio);
                }
            } else if (buffer > 0) buffer-= 0.2;

            debug("r=%.4f smallest=%s f=%s lf=%s pm=%.5f dxz=%.5f b=%.1f f/s=%.2f,%.2f soulsand=%s ", ratio, smallestDelta, frictionList, lfrictionList, pmotion,
                    player.getMovement().getDeltaXZ(), buffer, forward, strafe,
                    player.getBlockInfo().onSoulSand);

            pvX = pmotionx * (0.9100000262260437 * (player.getMovement().getFrom().isOnGround() ? friction : 1));
            pvZ = pmotionz * (0.9100000262260437 * (player.getMovement().getFrom().isOnGround() ? friction : 1));
        }
        previousFrom = player.getMovement().getFrom().getLoc().clone();
    };

    private static final float[] SIN_TABLE_FAST = new float[4096], SIN_TABLE_FAST_NEW = new float[4096];
    private static final float[] SIN_TABLE = new float[65536];
    private static final float radToIndex = roundToFloat(651.8986469044033D);

    public static float sin(int type, float value) {
        switch (type) {
            case 0:
            default: {
                return SIN_TABLE[(int) (value * 10430.378F) & 65535];
            }
            case 1: {
                return SIN_TABLE_FAST[(int) (value * 651.8986F) & 4095];
            }
            case 2: {
                return SIN_TABLE_FAST_NEW[(int) (value * radToIndex) & 4095];
            }
        }
    }

    public static float cos(int type, float value) {
        switch (type) {
            case 0:
            default:
                return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
            case 1:
                return SIN_TABLE_FAST[(int) ((value + ((float) Math.PI / 2F)) * 651.8986F) & 4095];
            case 2:
                return SIN_TABLE_FAST_NEW[(int) (value * radToIndex + 1024.0F) & 4095];
        }
    }

    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
        }

        for (int j = 0; j < 4096; ++j) {
            SIN_TABLE_FAST[j] = (float) Math.sin((double) (((float) j + 0.5F) / 4096.0F * ((float) Math.PI * 2F)));
        }

        for (int l = 0; l < 360; l += 90) {
            SIN_TABLE_FAST[(int) ((float) l * 11.377778F) & 4095] = (float) Math.sin((double) ((float) l * 0.017453292F));
        }

        for (int j = 0; j < SIN_TABLE_FAST_NEW.length; ++j) {
            SIN_TABLE_FAST_NEW[j] = roundToFloat(Math.sin((double) j * Math.PI * 2.0D / 4096.0D));
        }
    }

    private static float roundToFloat(double d) {
        return (float) ((double) Math.round(d * 1.0E8D) / 1.0E8D);
    }
}
