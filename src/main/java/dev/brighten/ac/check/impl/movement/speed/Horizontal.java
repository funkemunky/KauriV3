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
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import dev.brighten.ac.utils.wrapper.Wrapper;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Horizontal", checkId = "horizontala", type = CheckType.MOVEMENT)
public class Horizontal extends Check {
    private boolean lastLastClientGround, stepped;
    private float buffer, vbuffer;
    private boolean maybeSkippedPos;
    private Timer lastSkipPos = new TickTimer();
    private int lastFlying;
    
    private KLocation previousFrom;
    private Vector motionX = new Vector(0,0,0);
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
                    || player.getMovement().getLastTeleport().isNotPassed(1)
                    || player.getInfo().getVelocity().isNotPassed(2)
                    || player.getInfo().isGeneralCancel()
                    || player.getBlockInfo().onClimbable
                    || player.getMovement().getTo().getLoc().toVector()
                    .distanceSquared(player.getMovement().getFrom().getLoc().toVector()) > 2500
                    || player.getInfo().lastLiquid.isNotPassed(2)) {
                motionX = new Vector(player.getMovement().getDeltaX(), player.getMovement().getDeltaY(),
                        player.getMovement().getDeltaZ());
                break check;
            }

            double smallDelta = Double.MAX_VALUE, smallestDeltaXZ = Double.MAX_VALUE, smallestDeltaY = Double.MAX_VALUE;

            double pmotionx = 0, pmotiony = 0, pmotionz = 0;
            boolean onGround = player.getMovement().getFrom().isOnGround();

            List<Iteration> iterations = getIteration();

            boolean found = false;
            double precision = getPrecision();
            TagsBuilder tags = null;

            for (Iteration it : iterations) {
                TagsBuilder tagsBuilder = new TagsBuilder();
                float forward = it.f, strafe = it.s;

                if (it.sprinting && forward <= 0) {
                    continue;
                }

                if (it.sneaking) {
                    tagsBuilder.addTag("sneak");
                    forward *= 0.3;
                    strafe *= 0.3;
                }

                float friction = Wrapper.getInstance().getFriction(it.underMaterial);
                float lfriction = Wrapper.getInstance().getFriction(it.lastUnderMaterial);

                if (it.using) {
                    tagsBuilder.addTag("itemUse");
                    forward *= 0.2;
                    strafe *= 0.2;
                }

                //Multiplying by 0.98 like in client
                forward *= 0.9800000190734863F;
                strafe *= 0.9800000190734863F;

                double aiMoveSpeed = player.getBukkitPlayer().getWalkSpeed() / 2;

                float drag = 0.91f;
                double lmotionX = motionX.getX(),
                        lmotionY = motionX.getY(),
                        lmotionZ = motionX.getZ();

                lmotionY-= 0.08;
                lmotionY*= 0.98f;

                if(player.getBlockInfo().onSoulSand
                        && player.getBlockInfo().collisionMaterialCount.
                        containsKey(Material.SOUL_SAND)) {

                    int i;
                    for(i = 0
                        ; i < player.getBlockInfo()
                            .collisionMaterialCount
                            .get(Material.SOUL_SAND)
                            ; i++) {
                        lmotionX*= 0.4;
                        lmotionZ*= 0.4;
                    }

                    if(i > 0) {
                        tagsBuilder.addTag("soulSand:" + i);
                    }
                }

                if(player.getBlockInfo().inWeb) {
                    lmotionX*= 0.25;
                    lmotionZ*= 0.25;
                    tagsBuilder.addTag("web");
                }

                //The "1" will effectively remove lastFriction from the equation
                lmotionX *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;
                lmotionZ *= (lastLastClientGround ? lfriction : 1) * 0.9100000262260437D;

                //Running multiplication done after previous prediction
                if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                    if (Math.abs(lmotionX) < 0.003) {
                        lmotionX = 0;
                        tagsBuilder.addTag("motionXZero");
                    }
                    if (Math.abs(lmotionZ) < 0.003) {
                        lmotionZ = 0;
                        tagsBuilder.addTag("motionZZero");
                    }
                } else {
                    if (Math.abs(lmotionX) < 0.005) {
                        lmotionX = 0;
                        tagsBuilder.addTag("motionXZero");
                    }
                    if (Math.abs(lmotionZ) < 0.005) {
                        lmotionZ = 0;
                        tagsBuilder.addTag("motionZZero");
                    }
                }

                // Attack slowdown
                if (it.attack) {
                    lmotionX *= 0.6;
                    lmotionZ *= 0.6;
                    tagsBuilder.addTag("attackSlow");
                }

                if (it.sprinting) {
                    aiMoveSpeed += aiMoveSpeed * 0.30000001192092896D;
                    tagsBuilder.addTag("sprinting");
                }

                if (player.getPotionHandler().hasPotionEffect(PotionEffectType.SPEED)) {
                    aiMoveSpeed += (player.getPotionHandler().getEffectByType(PotionEffectType.SPEED)
                            .get()
                            .getAmplifier() + 1) * (double) 0.20000000298023224D * aiMoveSpeed;

                    tagsBuilder.addTag("speedPotion");
                }
                if (player.getPotionHandler().hasPotionEffect(PotionEffectType.SLOW)) {
                    aiMoveSpeed += (player.getPotionHandler().getEffectByType(PotionEffectType.SLOW)
                            .get()
                            .getAmplifier() + 1) * (double) -0.15000000596046448D * aiMoveSpeed;
                    tagsBuilder.addTag("slowPotion");
                }

                float f5;
                if (onGround) {
                    tagsBuilder.addTag("ground");
                    drag *= friction;

                    f5 = (float) (aiMoveSpeed * (0.16277136F / (drag * drag * drag)));

                    if (it.jumped) {
                        if(it.sprinting) {
                            float rot = player.getMovement().getTo().getLoc().yaw * 0.017453292F;
                            lmotionX -= sin(it.fastMath, rot) * 0.2F;
                            lmotionZ += cos(it.fastMath, rot) * 0.2F;
                        }

                        lmotionY = MovementUtils.getJumpHeight(player);

                        tagsBuilder.addTag("jumped");
                    }

                } else {
                    tagsBuilder.addTag("air");
                    f5 = it.sprinting ? 0.025999999F : 0.02f;
                }

                if (player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_9)) {
                    double keyedMotion = forward * forward + strafe * strafe;

                    if (keyedMotion >= 1.0E-4F) {
                        keyedMotion = f5 / Math.max(1.0, Math.sqrt(keyedMotion));
                        forward *= keyedMotion;
                        strafe *= keyedMotion;

                        final float yawSin = sin(it.fastMath,
                                player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                yawCos = cos(it.fastMath,
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

                        final float yawSin = sin(it.fastMath,
                                player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F),
                                yawCos = cos(it.fastMath,
                                        player.getMovement().getTo().getLoc().yaw * (float) Math.PI / 180.F);

                        lmotionX += (strafe * yawCos - forward * yawSin);
                        lmotionZ += (forward * yawCos + strafe * yawSin);
                    }
                }

                if(Math.abs(lmotionY) < (player.getPlayerVersion().isBelow(ProtocolVersion.V1_9) ? 0.005 : 0.003)) {
                    lmotionY = 0;
                }

                double originalX = lmotionX, originalY = lmotionY, originalZ = lmotionZ;

                SimpleCollisionBox box = player.getMovement().getFrom().getBox().copy();

                if(it.sneaking && onGround) {
                    double d6;

                    // 1.9+ changes from 0.05 to 0.03
                    double protocolOffset = player.getPlayerVersion().isBelow(ProtocolVersion.V1_9) ?  0.05D : 0.03D;

                    for (d6 = protocolOffset; lmotionX != 0.0D && Helper.getCollisions(player, box.copy()
                            .offset(lmotionX, 0 ,0)).isEmpty(); originalX = lmotionX) {
                        if (lmotionX < d6 && lmotionX >= -d6) {
                            lmotionX = 0.0D;
                        } else if (lmotionX > 0.0D) {
                            lmotionX -= d6;
                        } else {
                            lmotionX += d6;
                        }
                    }

                    for (; lmotionZ != 0.0D && Helper.getCollisions(player, box.copy()
                            .offset(0, 0 ,lmotionZ)).isEmpty(); originalZ = lmotionZ) {
                        if (lmotionZ < d6 && lmotionZ >= -d6) {
                            lmotionZ = 0.0D;
                        } else if (lmotionZ > 0.0D) {
                            lmotionZ -= d6;
                        } else {
                            lmotionZ += d6;
                        }
                    }

                    for (; lmotionX != 0.0D && lmotionZ != 0.0D && Helper.getCollisions(player, box.copy()
                            .offset(lmotionX, -1.0 ,lmotionZ)).isEmpty();
                         originalZ = lmotionZ) {
                        if (lmotionX < d6 && lmotionX >= -d6) {
                            lmotionX = 0.0D;
                        } else if (lmotionX > 0.0D) {
                            lmotionX -= d6;
                        } else {
                            lmotionX += d6;
                        }

                        originalX = lmotionX;

                        if (lmotionZ < d6 && lmotionZ >= -d6) {
                            lmotionZ = 0.0D;
                        } else if (lmotionZ > 0.0D) {
                            lmotionZ -= d6;
                        } else {
                            lmotionZ += d6;
                        }
                    }
                    tagsBuilder.addTag("sneak-edge");
                }

                List<SimpleCollisionBox> collisionBoxes = Helper.getCollisions(player, box.copy()
                        .addCoord(lmotionX ,lmotionY, lmotionZ));

                for (SimpleCollisionBox blockBox : collisionBoxes) {
                    lmotionY = blockBox.calculateYOffset(box, lmotionY);
                }

                box = box.offset(0, lmotionY, 0);

                boolean stepped = onGround || (originalY != lmotionY && originalY < 0);
                
                for (SimpleCollisionBox blockBox : collisionBoxes) {
                    lmotionX = blockBox.calculateXOffset(box, lmotionX);
                }

                box = box.offset(lmotionX,0,0);
                for (SimpleCollisionBox blockBox : collisionBoxes) {
                    lmotionZ = blockBox.calculateZOffset(box, lmotionZ);
                }

                box = box.offset(0, 0, lmotionZ);

                if(stepped && (lmotionX != originalX || lmotionZ != originalZ)) {
                    double d11 = lmotionX;
                    double d7 = lmotionY;
                    double d8 = lmotionZ;
                    SimpleCollisionBox axisalignedbb3 = box;

                    box = player.getMovement().getFrom().getBox().copy();

                    lmotionY = 0.6; //Step height
                    List<SimpleCollisionBox> list = Helper.getCollisions(player,
                            box.copy().addCoord(originalX, lmotionY, originalZ));
                    SimpleCollisionBox axisalignedbb4 = box;
                    SimpleCollisionBox axisalignedbb5 = axisalignedbb4.copy().addCoord(originalX, 0.0D, originalZ);
                    double d9 = lmotionY;

                    for (SimpleCollisionBox axisalignedbb6 : list) {
                        d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);
                    }

                    axisalignedbb4 = axisalignedbb4.copy().offset(0.0D, d9, 0.0D);
                    double d15 = originalX;

                    for (SimpleCollisionBox axisalignedbb7 : list) {
                        d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15);
                    }

                    axisalignedbb4 = axisalignedbb4.offset(d15, 0.0D, 0.0D);

                    double d16 = originalZ;

                    for (SimpleCollisionBox axisalignedbb8 : list) {
                        d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16);
                    }

                    axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d16);
                    SimpleCollisionBox axisalignedbb14 = box;
                    double d17 = lmotionY;

                    for (SimpleCollisionBox axisalignedbb9 : list) {
                        d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17);
                    }

                    axisalignedbb14 = axisalignedbb14.copy().offset(0.0D, d17, 0.0D);
                    double d18 = originalX;

                    for (SimpleCollisionBox axisalignedbb10 : list) {
                        d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18);
                    }

                    axisalignedbb14 = axisalignedbb14.copy().offset(d18, 0.0D, 0.0D);
                    double d19 = originalZ;

                    for (SimpleCollisionBox axisalignedbb11 : list) {
                        d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19);
                    }

                    axisalignedbb14 = axisalignedbb14.copy().offset(0.0D, 0.0D, d19);
                    double d20 = d15 * d15 + d16 * d16;
                    double d10 = d18 * d18 + d19 * d19;

                    if (d20 > d10) {
                        lmotionX = d15;
                        lmotionZ = d16;
                        lmotionY = -d9;
                        box = axisalignedbb4;
                    } else {
                        lmotionX = d18;
                        lmotionZ = d19;
                        lmotionY = -d17;
                        box = axisalignedbb14;
                    }

                    for (SimpleCollisionBox axisalignedbb12 : list) {
                        lmotionY = axisalignedbb12.calculateYOffset(box, lmotionY);
                    }

                    box = box.copy().offset(0.0D, lmotionY, 0.0D);

                    if (d11 * d11 + d8 * d8 >= lmotionX * lmotionX + lmotionZ * lmotionZ) {
                        lmotionX = d11;
                        lmotionY = d7;
                        lmotionZ = d8;
                        box = axisalignedbb3;
                    }

                    tagsBuilder.addTag("stepped");
                }

                double x = ((box.minX + box.maxX) / 2.0D) - player.getMovement().getFrom().getX();
                double y = box.minY - player.getMovement().getFrom().getY();
                double z = ((box.minZ + box.maxZ) / 2.0D) - player.getMovement().getFrom().getZ();

                if (originalX != lmotionX) {
                     lmotionX = 0.0D;
                    tagsBuilder.addTag("x-collision");
                }

                if (originalY != lmotionY) {
                     lmotionY = 0.0D;
                    tagsBuilder.addTag("y-collision");
                }

                if (originalZ != lmotionZ) {
                     lmotionZ = 0.0D;
                    tagsBuilder.addTag("z-collision");
                }

                double diffX = player.getMovement().getDeltaX() - x,
                        diffY = player.getMovement().getDeltaY() - y,
                        diffZ = player.getMovement().getDeltaZ() - z;
                double delta = (diffX * diffX) + (diffZ * diffZ);
                double deltaAll = delta + (diffY * diffY);
                double deltaY = Math.abs(diffY);

                if (deltaAll < smallDelta) {
                    smallDelta = deltaAll;
                    smallestDeltaXZ = delta;
                    smallestDeltaY = deltaY;
                    pmotionx = x;
                    pmotiony = y;
                    pmotionz = z;

                    tags = tagsBuilder;

                    if (deltaAll < precision) {
                        this.strafe = it.s * 0.98f;
                        this.forward = it.f * 0.98f;

                        if(precision < 1E-11)
                            motionX = new Vector(lmotionX, lmotionY, lmotionZ);
                        else  motionX = new Vector(player.getMovement().getDeltaX(), player.getMovement().getDeltaY(), player.getMovement().getDeltaZ());

                        if (player.getInfo().getLastCancel().isPassed(2))
                            player.getInfo()
                                    .setLastKnownGoodPosition(player
                                            .getMovement().getFrom().getLoc()
                                            .clone());
                    }
                }
            }
            iterations.clear();

            if(!found) {
                motionX = new Vector(player.getMovement().getDeltaX(), player.getMovement().getDeltaY(), player.getMovement().getDeltaZ());
            }
            double pmotion = Math.hypot(pmotionx, pmotionz);

            final String builtTags = tags == null ? "null" : tags.build();

            if (smallestDeltaXZ > (precision)
                    && player.getMovement().getDeltaXZ() > 0.1) {
                if ((buffer += smallestDeltaXZ > 58E-5 ? 1 : 0.5) > 1) {
                    buffer = Math.min(3.5f, buffer); //Ensuring we don't have a run-away buffer
                    flag("smallest=%s b=%.1f to=%s dxz=%.2f tags=[%s]", smallestDeltaXZ, buffer,
                            player.getMovement().getTo().getLoc(), player.getMovement().getDeltaXZ(), builtTags);
                    cancel();
                } else debug("bad movement");

            } else if (buffer > 0) buffer -= 0.05f;

            if(smallestDeltaY > precision
                    && !player.getBlockInfo().onStairs && !player.getBlockInfo().nearSteppableEntity) {
                double finalSmallestDeltaY = smallestDeltaY;
                if(++vbuffer > 1) {
                    cancel();
                    find(Vertical.class)
                            .ifPresent(vc -> vc.flag("dy=%.4f;sd=%s tags=[%s]",
                                    player.getMovement().getDeltaY(), finalSmallestDeltaY, builtTags));
                }
            } else if(vbuffer > 0) vbuffer-= 0.05f;

            debug("(%s): py=%.5f dy=%.5f pm=%.5f dxz=%.5f skip=%s b=%.1f vb=%.1f tags=[%s]",
                   precision, pmotiony, player.getMovement().getDeltaY(), pmotion,
                    player.getMovement().getDeltaXZ(), maybeSkippedPos, buffer, vbuffer, builtTags);
        }

        if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
            maybeSkippedPos = !packet.isMoved();
        } else {
            if(player.getPlayerTick() - lastFlying > 1) {
                maybeSkippedPos = true;
                debug("maybe skipped pos");
            }
            lastFlying = player.getPlayerTick();
        }

        if(maybeSkippedPos) {
            lastSkipPos.reset();
        }
        lastLastClientGround = player.getMovement().getFrom().isOnGround();
        previousFrom = player.getMovement().getFrom().getLoc().clone();
    };

    private double getPrecision() {
        if(player.getBlockInfo().onSoulSand) {
            return 5E-4;
        } else if(lastSkipPos.isNotPassed(2)) {
            return 0.03;
        }
        return 5E-13;
    }

    private List<Iteration> getIteration() {
        List<Iteration> iterations = new ArrayList<>();
        val underBlockLoc = previousFrom != null ? player.getMovement().getFrom().getLoc() : player.getMovement().getTo().getLoc();
        val lastUnderBlockLoc = previousFrom != null ? previousFrom : player.getMovement().getFrom().getLoc();

        Material underMaterial = player.getBlockUpdateHandler()
                .getBlock(new IntVector(MathHelper.floor_double(underBlockLoc.x),
                        MathHelper.floor_double(underBlockLoc.y - 1), MathHelper.floor_double(underBlockLoc.z))).getType(),
                lastUnderMaterial = player.getBlockUpdateHandler()
                        .getBlock(new IntVector(MathHelper.floor_double(lastUnderBlockLoc.x),
                                MathHelper.floor_double(lastUnderBlockLoc.y - 1),
                                MathHelper.floor_double(lastUnderBlockLoc.z))).getType();
        for (int f = -1; f < 2; f++) {
            for (int s = -1; s < 2; s++) {
                for (boolean sprinting : getSprintIteration(f)) {
                    for (int fastMath = 0; fastMath <= 2; fastMath++) {
                        for (boolean attack : TRUE_FALSE) {
                            for (boolean using : TRUE_FALSE) {
                                for (boolean sneaking : getSneakingIteration(sprinting)) {
                                    for (boolean jumped : getJumpingIteration(player.getMovement().getFrom().isOnGround())) {
                                        iterations.add(new Iteration(underMaterial, lastUnderMaterial, f, s,
                                                fastMath, sprinting, attack, using, sneaking, jumped));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return iterations;
    }


    @AllArgsConstructor
    private static class Iteration {
       public Material underMaterial, lastUnderMaterial;
       public int f, s, fastMath;
       public boolean sprinting, attack, using, sneaking, jumped;
    }

    private static boolean[] getSprintIteration(int f) {
        if(f > 0) {
            return new boolean[] {true, false};
        }

        return new boolean[] {false};
    }

    private static boolean[] getSneakingIteration(boolean sprinting) {
        return new boolean[] {true, false};
    }

    private static boolean[] getJumpingIteration(boolean onGround) {
        return onGround ? new boolean[] {true, false} : new boolean[] {false};
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