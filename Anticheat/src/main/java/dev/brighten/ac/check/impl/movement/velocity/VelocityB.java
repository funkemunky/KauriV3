package dev.brighten.ac.check.impl.movement.velocity;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.annotation.Bind;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import lombok.val;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Velocity (Horizontal)", checkId = "velocityb", type = CheckType.MOVEMENT)
public class VelocityB extends Check {
    private final Timer lastVelocity = new TickTimer();

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
    private int ticks;
    private double buffer;
    private float friction;
    private KLocation previousFrom;
    private final boolean[] TRUE_FALSE = new boolean[]{true, false};

    public double strafe, forward;

    private final List<Iteration> iterations = new ArrayList<>();

    @Bind
    WAction<WrapperPlayClientPlayerFlying> flying = packet -> {

        check:
        {
            if (ticks == 0) break check;
            val underBlockLoc = previousFrom != null
                    ? player.getMovement().getFrom().getLoc() : player.getMovement().getTo().getLoc();

            Material underMaterial = player.getBlockUpdateHandler()
                    .getBlock(new IntVector(MathHelper.floor_double(underBlockLoc.getX()),
                            MathHelper.floor_double(underBlockLoc.getY() - 1),
                            MathHelper.floor_double(underBlockLoc.getZ())))
                    .getType();

            if (player.getMovement().getMoveTicks() == 0
                    || player.getInfo().isGeneralCancel()
                    || player.getBlockInfo().onClimbable
                    || player.getInfo().lastWeb.isNotPassed(2)
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getBlockInfo().collidesHorizontally) {
                ticks = 0;
                break check;
            }

            // Too small of velocity
            if(MathUtils.hypotSqrt(pvX, pvZ) < 0.025) {
                pvX = pvZ = 0;
                ticks = 0;
                break check;
            }
            double smallestDelta = Double.MAX_VALUE;

            double pmotionx = 0, pmotionz = 0;
            boolean onGround = player.getMovement().getFrom().isOnGround();

            val speed = player.getPotionHandler().getEffectByType(PotionEffectType.SPEED);
            val slow = player.getPotionHandler().getEffectByType(PotionEffectType.SLOW);

            for (Iteration iteration : iterations) {
                float forward = iteration.f, strafe = iteration.s;

                if (iteration.sneaking) {
                    forward *= 0.3F;
                    strafe *= 0.3F;
                }

                float friction = CraftMagicNumbers.getBlock(underMaterial).frictionFactor;

                if (iteration.using) {
                    forward *= 0.2F;
                    strafe *= 0.2F;
                }

                //Multiplying by 0.98 like in client
                forward *= 0.9800000190734863F;
                strafe *= 0.9800000190734863F;

                double aiMoveSpeed = player.getBukkitPlayer().getWalkSpeed() / 2;

                float drag = 0.91f;
                double lmotionX = pvX,
                        lmotionZ = pvZ;

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

                // Attack slowdown
                if (iteration.attack) {
                    lmotionX *= 0.6;
                    lmotionZ *= 0.6;
                }

                if (iteration.sprinting)
                    aiMoveSpeed += aiMoveSpeed * 0.30000001192092896D;

                if (speed.isPresent())
                    aiMoveSpeed += (speed.get().getAmplifier() + 1) * 0.20000000298023224D * aiMoveSpeed;
                if (slow.isPresent())
                    aiMoveSpeed += (slow.get().getAmplifier() + 1) * -0.15000000596046448D * aiMoveSpeed;

                float f5;
                if (onGround) {
                    drag *= friction;

                    f5 = (float) (aiMoveSpeed * (0.16277136F / (drag * drag * drag)));

                    if (iteration.sprinting && iteration.jumped) {
                        float rot = player.getMovement().getTo().getLoc().getYaw() * 0.017453292F;
                        lmotionX -= sin(iteration.fastMath, rot) * 0.2F;
                        lmotionZ += cos(iteration.fastMath, rot) * 0.2F;
                    }

                } else f5 = iteration.sprinting ? 0.025999999F : 0.02f;

                if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                    double keyedMotion = forward * forward + strafe * strafe;

                    if (keyedMotion >= 1.0E-4F) {
                        keyedMotion = f5 / Math.max(1.0, Math.sqrt(keyedMotion));
                        forward *= (float) keyedMotion;
                        strafe *= (float) keyedMotion;

                        final float yawSin = sin(iteration.fastMath,
                                player.getMovement().getTo().getLoc().getYaw() * (float) Math.PI / 180.F),
                                yawCos = cos(iteration.fastMath,
                                        player.getMovement().getTo().getLoc().getYaw() * (float) Math.PI / 180.F);

                        lmotionX += (strafe * yawCos - forward * yawSin);
                        lmotionZ += (forward * yawCos + strafe * yawSin);
                    }
                } else {
                    float keyedMotion = forward * forward + strafe * strafe;

                    if (keyedMotion >= 1.0E-4F) {
                        keyedMotion = f5 / Math.max(1.0f, MathHelper.sqrt_float(keyedMotion));
                        forward *= keyedMotion;
                        strafe *= keyedMotion;

                        final float yawSin = sin(iteration.fastMath,
                                player.getMovement().getTo().getLoc().getYaw() * (float) Math.PI / 180.F),
                                yawCos = cos(iteration.fastMath,
                                        player.getMovement().getTo().getLoc().getYaw() * (float) Math.PI / 180.F);

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
                        this.strafe = iteration.s * 0.98f;
                        this.forward = iteration.f * 0.98f;

                        break;
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
            } else if (buffer > 0) buffer -= 0.2;

            debug("r=%.4f smallest=%s pm=%.5f dxz=%.5f b=%.1f f/s=%.2f,%.2f soulsand=%s ",
                    ratio, smallestDelta, pmotion, player.getMovement().getDeltaXZ(), buffer, forward, strafe,
                    player.getBlockInfo().onSoulSand);

            pvX = pmotionx * (0.9100000262260437 * (player.getMovement().getFrom().isOnGround() ? friction : 1));
            pvZ = pmotionz * (0.9100000262260437 * (player.getMovement().getFrom().isOnGround() ? friction : 1));
        }
        previousFrom = player.getMovement().getFrom().getLoc().clone();
    };

    private record Iteration(int f, int s, int fastMath, boolean sprinting, boolean attack, boolean using,
                                 boolean sneaking, boolean jumped) {
    }

    {
        for (int f = -1; f < 2; f++) {
            for (int s = -1; s < 2; s++) {
                for (boolean sprinting : TRUE_FALSE) {
                    for (int fastMath = 0; fastMath <= 2; fastMath++) {
                        for (boolean attack : TRUE_FALSE) {
                            for (boolean using : TRUE_FALSE) {
                                for (boolean sneaking : TRUE_FALSE) {
                                    for (boolean jumped : TRUE_FALSE) {
                                        if(sprinting && f <= 0) continue;
                                        if(jumped && !sprinting) continue;
                                        iterations.add(new Iteration(f, s, fastMath, sprinting,
                                                attack, using, sneaking, jumped));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private final float[] SIN_TABLE_FAST = new float[4096], SIN_TABLE_FAST_NEW = new float[4096];
    private final float[] SIN_TABLE = new float[65536];
    private final float radToIndex = roundToFloat(651.8986469044033D);

    public float sin(int type, float value) {
        return switch (type) {
            case 1 -> SIN_TABLE_FAST[(int) (value * 651.8986F) & 4095];
            case 2 -> SIN_TABLE_FAST_NEW[(int) (value * radToIndex) & 4095];
            default -> SIN_TABLE[(int) (value * 10430.378F) & 65535];
        };
    }

    public float cos(int type, float value) {
        return switch (type) {
            case 1 -> SIN_TABLE_FAST[(int) ((value + ((float) Math.PI / 2F)) * 651.8986F) & 4095];
            case 2 -> SIN_TABLE_FAST_NEW[(int) (value * radToIndex + 1024.0F) & 4095];
            default -> SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
        };
    }

    {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
        }

        for (int j = 0; j < 4096; ++j) {
            SIN_TABLE_FAST[j] = (float) Math.sin(((float) j + 0.5F) / 4096.0F * ((float) Math.PI * 2F));
        }

        for (int l = 0; l < 360; l += 90) {
            SIN_TABLE_FAST[(int) ((float) l * 11.377778F) & 4095] = (float) Math.sin((float) l * 0.017453292F);
        }

        for (int j = 0; j < SIN_TABLE_FAST_NEW.length; ++j) {
            SIN_TABLE_FAST_NEW[j] = roundToFloat(Math.sin((double) j * Math.PI * 2.0D / 4096.0D));
        }
    }

    private float roundToFloat(double d) {
        return (float) ((double) Math.round(d * 1.0E8D) / 1.0E8D);
    }
}
