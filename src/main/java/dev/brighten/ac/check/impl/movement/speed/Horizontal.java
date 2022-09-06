package dev.brighten.ac.check.impl.movement.speed;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathHelper;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.wrapper.Wrapper;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.stream.Collectors;

@CheckData(name = "Horizontal", checkId = "horizontala", type = CheckType.MOVEMENT)
public class Horizontal extends Check {
    private boolean lastLastClientGround;
    private float buffer;
    private Vector velocity;
    private int vTicks;
    private KLocation previousFrom;
    private static final boolean[] TRUE_FALSE = new boolean[]{true, false};

    public double strafe, forward;

    public Horizontal(APlayer player) {
        super(player);
    }

    WAction<WPacketPlayInFlying> flying = packet -> {

        check:
        {

            if (!packet.isMoved()
                    || player.getMovement().getMoveTicks() == 0
                    || player.getInfo().getVelocity().isNotPassed(2)
                    || player.getInfo().isGeneralCancel()
                    || player.getBlockInfo().onClimbable
                    || player.getInfo().lastLiquid.isNotPassed(2)
                    || player.getBlockInfo().collidesHorizontally) {
                break check;
            }

            val underBlockLoc = previousFrom != null ? player.getMovement().getFrom().getLoc() : player.getMovement().getTo().getLoc();
            val lastUnderBlockLoc = previousFrom != null ? previousFrom : player.getMovement().getFrom().getLoc();

            Deque<Material> frictionList = player.getBlockUpdateHandler()
                    .getPossibleMaterials(new IntVector(MathHelper.floor_double(underBlockLoc.x),
                            MathHelper.floor_double(underBlockLoc.y), MathHelper.floor_double(underBlockLoc.z))),
                    lfrictionList = player.getBlockUpdateHandler()
                            .getPossibleMaterials(new IntVector(MathHelper.floor_double(lastUnderBlockLoc.x),
                                    MathHelper.floor_double(lastUnderBlockLoc.y),
                                    MathHelper.floor_double(lastUnderBlockLoc.z)));

            double smallestDelta = Double.MAX_VALUE;

            double pmotionx = 0, pmotionz = 0;
            boolean onGround = player.getMovement().getFrom().isOnGround();

            loop:
            {
                for (int f = -1; f < 2; f++) {
                    for (Material underMaterial : frictionList) {
                        for (Material lastUnderMaterial : lfrictionList) {
                            for (int s = -1; s < 2; s++) {
                                for (boolean sprinting : getSprintIteration(f)) {
                                    for (int fastMath = 0; fastMath <= 2; fastMath++) {
                                        for (boolean attack : TRUE_FALSE) {
                                            for (boolean motionModifiers : TRUE_FALSE) {
                                                for (boolean using : TRUE_FALSE) {
                                                    for (boolean sneaking : getSneakingIteration(sprinting)) {
                                                        for (boolean jumped : getJumpingIteration(sprinting)) {

                                                            float forward = f, strafe = s;

                                                            if (sprinting && forward <= 0) {
                                                                continue;
                                                            }

                                                            if (sneaking) {
                                                                forward *= 0.3;
                                                                strafe *= 0.3;
                                                            }

                                                            float friction = Wrapper.getInstance().getFriction(underMaterial);
                                                            float lfriction = Wrapper.getInstance().getFriction(lastUnderMaterial);

                                                            if (using) {
                                                                forward *= 0.2;
                                                                strafe *= 0.2;
                                                            }

                                                            //Multiplying by 0.98 like in client
                                                            forward *= 0.9800000190734863F;
                                                            strafe *= 0.9800000190734863F;

                                                            double aiMoveSpeed = player.getBukkitPlayer().getWalkSpeed() / 2;

                                                            float drag = 0.91f;
                                                            double lmotionX = player.getMovement().getLDeltaX(),
                                                                    lmotionZ = player.getMovement().getLDeltaZ();

                                                            if(motionModifiers) {
                                                                if(player.getBlockInfo().onSoulSand
                                                                        && player.getBlockInfo().collisionMaterialCount.
                                                                        containsKey(Material.SOUL_SAND)) {

                                                                    for(int i = 0
                                                                        ; i < player.getBlockInfo()
                                                                            .collisionMaterialCount
                                                                            .get(Material.SOUL_SAND)
                                                                            ; i++) {
                                                                        lmotionX*= 0.4;
                                                                        lmotionZ*= 0.4;
                                                                    }
                                                                }

                                                                if(player.getBlockInfo().inWeb) {
                                                                    lmotionX*= 0.25;
                                                                    lmotionZ*= 0.25;
                                                                }
                                                            }

                                                            //The "1" will effectively remove lastFriction from the equation
                                                            lmotionX *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;
                                                            lmotionZ *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;

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
                                                            if(((lmotionX * lmotionX) + (lmotionZ * lmotionZ)) < 0.0025 && player.getMovement().getDeltaXZ() < 0.2) {
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

                                                                if (delta < 4E-17) {
                                                                    this.strafe = s * 0.98f;
                                                                    this.forward = f * 0.98f;

                                                                    if (player.getInfo().getLastCancel().isPassed(2))
                                                                        player.getInfo()
                                                                                .setLastKnownGoodPosition(player
                                                                                        .getMovement().getFrom().getLoc()
                                                                                        .clone());
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
                }
            }

            double pmotion = Math.hypot(pmotionx, pmotionz);

            if (player.getMovement().getDeltaXZ() > pmotion
                    && smallestDelta > (player.getBlockInfo().onSoulSand ? 0.01 : 5E-13)
                    && player.getMovement().getDeltaXZ() > 0.1) {
                if ((buffer += smallestDelta > 58E-5 ? 1 : 0.5) > 1) {
                    buffer = Math.min(3.5f, buffer); //Ensuring we don't have a run-away buffer
                    flag("smallest=%s b=%.1f to=%s dxz=%.2f", smallestDelta, buffer,
                            player.getMovement().getTo().getLoc(), player.getMovement().getDeltaXZ());
                    cancel();
                } else debug("bad movement");
            } else if (buffer > 0) buffer -= 0.05f;

            debug("smallest=%s sp=%s efcs=[%s] pm=%.5f dxz=%.5f b=%.1f", smallestDelta,
                    player.getInfo().isSprinting(), player.getPotionHandler().potionEffects.stream()
                            .map(pe -> pe.getType().getName() + ";" + pe.getAmplifier())
                            .collect(Collectors.joining(", ")), pmotion,
                    player.getMovement().getDeltaXZ(), buffer);
        }
        lastLastClientGround = player.getMovement().getFrom().isOnGround();
        previousFrom = player.getMovement().getFrom().getLoc().clone();
    };


    private static boolean[] getSprintIteration(int f) {
        if(f > 0) {
            return new boolean[] {true, false};
        }

        return new boolean[] {false};
    }

    private static boolean[] getSneakingIteration(boolean sprinting) {
        return sprinting ? new boolean[] {false} : new boolean[] {true, false};
    }

    private static boolean[] getJumpingIteration(boolean sprinting) {
        return sprinting ? new boolean[] {true, false} : new boolean[] {false};
    }
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
