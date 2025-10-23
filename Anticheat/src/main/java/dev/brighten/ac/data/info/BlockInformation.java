package dev.brighten.ac.data.info;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.entity.TrackedEntity;
import dev.brighten.ac.utils.*;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import me.hydro.emulator.util.mcp.MathHelper;

import java.util.*;

public class BlockInformation {
    private final APlayer player;
    public boolean onClimbable, onSlab, onStairs, onHalfBlock, inLiquid, inLava, inWater, inWeb, onSlime, onIce,
            onSoulSand, blocksAbove, collidesVertically, bedNear, collidesHorizontally, blocksNear, inBlock, miscNear,
            collidedWithEntity, roseBush, fenceNear, inPortal, blocksBelow, pistonNear, fenceBelow, inScaffolding, inHoney,
            nearSteppableEntity;
    public final List<SimpleCollisionBox> aboveCollisions = Collections.synchronizedList(new ArrayList<>()),
            belowCollisions = Collections.synchronizedList(new ArrayList<>());
    public final List<CollisionBox> entityCollisionBoxes = new ArrayList<>();
    //Caching material
    public final Map<StateType, Integer> collisionMaterialCount = new HashMap<>();

    public BlockInformation(APlayer objectData) {
        this.player = objectData;
    }

    private SimpleCollisionBox newBox(double width, double height) {
        return new SimpleCollisionBox(player.getMovement().getTo().getLoc(), width, height);
    }

    public void runCollisionCheck() {
        if (!Anticheat.INSTANCE.isEnabled())
            return;

        double dy = Math.abs(player.getMovement().getDeltaY()) * 2;
        double dh = player.getMovement().getDeltaXZ() * 2;

        entityCollisionBoxes.clear();

        player.getInfo().setServerGround(false);
        player.getInfo().setNearGround(false);
        onClimbable = fenceBelow = inScaffolding = inHoney = nearSteppableEntity
                = onSlab = onStairs = onHalfBlock = inLiquid = inLava = inWater = inWeb = onSlime = pistonNear
                = onIce = onSoulSand = blocksAbove = collidesVertically = bedNear = collidesHorizontally =
                blocksNear = inBlock = miscNear = collidedWithEntity = blocksBelow = inPortal = false;

        collisionMaterialCount.clear();

        if (dy > 10) dy = 10;
        if (dh > 10) dh = 10;

        int startX = MathHelper.floor_double(player.getMovement().getTo().getLoc().getX() - 1 - dh);
        int endX = MathHelper.floor_double(player.getMovement().getTo().getLoc().getX() + 1 + dh);
        int startY = MathHelper.floor_double(player.getMovement().getTo().getLoc().getY() - 1 - dy);
        int endY = MathHelper.floor_double(player.getMovement().getTo().getLoc().getY() + 2.82 + dy);
        int startZ = MathHelper.floor_double(player.getMovement().getTo().getLoc().getZ() - 1 - dh);
        int endZ = MathHelper.floor_double(player.getMovement().getTo().getLoc().getZ() + 1 + dh);

        SimpleCollisionBox waterBox = player.getMovement().getTo().getBox().copy().expand(0, -.38, 0);

        waterBox.minX = MathHelper.floor_double(waterBox.minX);
        waterBox.minY = MathHelper.floor_double(waterBox.minY);
        waterBox.minZ = MathHelper.floor_double(waterBox.minZ);
        waterBox.maxX = MathHelper.floor_double(waterBox.maxX + 1.);
        waterBox.maxY = MathHelper.floor_double(waterBox.maxY + 1.);
        waterBox.maxZ = MathHelper.floor_double(waterBox.maxZ + 1.);

        SimpleCollisionBox lavaBox = player.getMovement().getTo().getBox().copy().expand(-.1f, -.4f, -.1f);

        lavaBox.minX = MathHelper.floor_double(waterBox.minX);
        lavaBox.minY = MathHelper.floor_double(waterBox.minY);
        lavaBox.minZ = MathHelper.floor_double(waterBox.minZ);
        lavaBox.maxX = MathHelper.floor_double(waterBox.maxX + 1.);
        lavaBox.maxY = MathHelper.floor_double(waterBox.maxY + 1.);
        lavaBox.maxZ = MathHelper.floor_double(waterBox.maxZ + 1.);

        SimpleCollisionBox normalBox = player.getMovement().getTo().getBox().copy();

        inWater = MiscUtils.isInMaterialBB(player, waterBox, Materials.WATER);
        inLava = MiscUtils.isInMaterialBB(player, lavaBox, Materials.LAVA);
        inLiquid = inWater || inLava;

        player.getInfo().setWorldLoaded(true);
        player.getInfo().setLastServerGround(player.getInfo().isServerGround());
        synchronized (belowCollisions) {
            belowCollisions.clear();
        }
        synchronized (aboveCollisions) {
            aboveCollisions.clear();
        }
        int it = 12 * 12;

        int xstart = Math.min(startX, endX), xend = Math.max(startX, endX);
        int zstart = Math.min(startZ, endZ), zend = Math.max(startZ, endZ);

        SimpleCollisionBox boundsForCollision = player.getMovement().getFrom().getBox() != null
                ? player.getMovement().getFrom().getBox().copy().shrink(0.001D, 0.001D, 0.001D)
                : null;

        Vector3i min;
        Vector3i max;

        if (boundsForCollision != null) {
            min = new Vector3i((int) boundsForCollision.minX, (int) boundsForCollision.minY, (int) boundsForCollision.minZ);
            max = new Vector3i((int) boundsForCollision.maxX, (int) boundsForCollision.maxY, (int) boundsForCollision.maxZ);
        } else {
            min = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            max = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        loop:
        {
            for (int x = xstart; x <= xend; ++x) {
                for (int z = zstart; z <= zend; ++z) {
                    for (int y = startY; y <= endY; ++y) {
                        if (it-- <= 0) {
                            break loop;
                        }

                        final StateType type =
                                player.getWorldTracker().getBlock(new Vector3i(x, y, z)).getType();

                        if (type != StateTypes.AIR) {

                            Vector3i vec = new Vector3i(x, y, z);
                            CollisionBox blockBox = BlockData.getData(type)
                                    .getBox(player, vec, player.getPlayerVersion());

                            // Checking of within boundsForCollision
                            if (x >= min.getX() && x <= max.getX()
                                    && y >= min.getY() && y <= max.getY()
                                    && z >= min.getZ() && z <= max.getZ()) {
                                collisionMaterialCount.compute(type, (key, count) -> {
                                    if (count == null) return 1;

                                    return count + 1;
                                });
                            }

                            if (player.isBoxDebug()) {
                                Anticheat.INSTANCE.getScheduler().execute(() -> blockBox.downCast().forEach(sc ->
                                        Helper.drawCuboid(sc,
                                                ParticleTypes.FLAME,
                                                Collections.singletonList(player))));
                            }

                            if (blockBox.isCollided(normalBox)) {
                                if (type.equals(StateTypes.COBWEB))
                                    inWeb = true;
                                else if (type.equals(StateTypes.SCAFFOLDING)) inScaffolding = true;
                                else if (type.equals(StateTypes.HONEY_BLOCK)) inHoney = true;
                            }

                            if (type.equals(StateTypes.ROSE_BUSH))
                                roseBush = true;

                            if (normalBox.copy().offset(0, 0.6f, 0).isCollided(blockBox))
                                blocksAbove = true;

                            if (normalBox.copy().expand(1, -0.0001, 1).isIntersected(blockBox))
                                blocksNear = true;

                            if (normalBox.copy().expand(0.1, 0, 0.1)
                                    .offset(0, 1, 0).isCollided(blockBox)) {
                                synchronized (aboveCollisions) {
                                    blockBox.downCast(aboveCollisions);
                                }
                            }

                            if (normalBox.copy().expand(0.1, 0, 0.1)
                                    .expandMax(0, -0.4, 0).expandMin(0, -0.55, 0)
                                    .isCollided(blockBox)) {
                                synchronized (belowCollisions) {
                                    blockBox.downCast(belowCollisions);
                                }
                            }

                            if (Materials.checkFlag(type, Materials.COLLIDABLE)) {
                                SimpleCollisionBox groundBox = newBox(0.6, 0.1)
                                        .expandMax(0, -0.5, 0);

                                if (Materials.checkFlag(type, Materials.FENCE)
                                        || Materials.checkFlag(type, Materials.WALL)) {
                                    fenceBelow = true;
                                }

                                if (newBox(1.4, 0).expandMin(0, -1, 0)
                                        .expand(0.3, 0, 0.3)
                                        .isIntersected(blockBox))
                                    blocksBelow = true;

                                if (normalBox.isIntersected(blockBox)) inBlock = true;

                                SimpleCollisionBox box = player.getMovement().getTo().getBox().copy();

                                box.expand(Math.abs(player.getMovement().getDeltaXZ() / 2) + 0.1, -0.001,
                                        Math.abs(player.getMovement().getDeltaXZ() / 2) + 0.1);
                                if (blockBox.isCollided(box))
                                    collidesHorizontally = true;

                                box = player.getMovement().getTo().getBox().copy();
                                box.expand(0, 0.1, 0);

                                if (blockBox.isCollided(box))
                                    collidesVertically = true;

                                if (groundBox.copy().expandMin(0, -0.8, 0)
                                        .isIntersected(blockBox))
                                    player.getInfo().setNearGround(true);

                                if (groundBox.copy().expandMin(0, -0.4, 0).isCollided(blockBox)) {
                                    player.getInfo().setServerGround(true);

                                    if (type.equals(StateTypes.ICE) || type.equals(StateTypes.BLUE_ICE)
                                            || type.equals(StateTypes.FROSTED_ICE)
                                            || type.equals(StateTypes.PACKED_ICE)) {
                                        if (groundBox.isCollided(blockBox))
                                            onIce = true;
                                    } else if (type.equals(StateTypes.SOUL_SAND)) {
                                        if (groundBox.isCollided(blockBox))
                                            onSoulSand = true;
                                    } else if (type.equals(StateTypes.SLIME_BLOCK)) {
                                        onSlime = true;
                                    }
                                }
                                if (player.getMovement().getDeltaY() > 0
                                        && player.getPlayerVersion().isOlderThan(ClientVersion.V_1_14)
                                        && Materials.checkFlag(type, Materials.LADDER)
                                        && normalBox.copy().expand(0.2f, 0, 0.2f)
                                        .isCollided(blockBox)) {
                                    onClimbable = true;
                                }

                                if (type.equals(StateTypes.PISTON)
                                        || type.equals(StateTypes.STICKY_PISTON)
                                        || type.equals(StateTypes.MOVING_PISTON)
                                        || type.equals(StateTypes.PISTON_HEAD)) {
                                    if (groundBox.copy().expand(0.5, 0.5, 0.5).isCollided(blockBox))
                                        pistonNear = true;
                                }

                                if (groundBox.copy().expand(0.5, 0.3, 0.5).isCollided(blockBox)) {
                                    if (Materials.checkFlag(type, Materials.SLABS))
                                        onSlab = true;
                                    else if (Materials.checkFlag(type, Materials.STAIRS))
                                        onStairs = true;
                                    else if (Materials.checkFlag(type, Materials.FENCE)
                                            || Materials.checkFlag(type, Materials.WALL)) {
                                        fenceNear = true;
                                    } else if (type.equals(StateTypes.CAKE)
                                            || type.equals(StateTypes.BREWING_STAND)
                                            || type.equals(StateTypes.FLOWER_POT)
                                            || type.equals(StateTypes.PLAYER_HEAD)
                                            || type.equals(StateTypes.PLAYER_WALL_HEAD)
                                            || type.equals(StateTypes.SKELETON_SKULL)
                                            || type.equals(StateTypes.CREEPER_HEAD)
                                            || type.equals(StateTypes.DRAGON_HEAD)
                                            || type.equals(StateTypes.ZOMBIE_HEAD)
                                            || type.equals(StateTypes.ZOMBIE_WALL_HEAD)
                                            || type.equals(StateTypes.CREEPER_WALL_HEAD)
                                            || type.equals(StateTypes.DRAGON_WALL_HEAD)
                                            || type.equals(StateTypes.WITHER_SKELETON_SKULL)
                                            || type.equals(StateTypes.LANTERN)
                                            || type.equals(StateTypes.SKELETON_WALL_SKULL)
                                            || type.equals(StateTypes.WITHER_SKELETON_WALL_SKULL)
                                            || type.equals(StateTypes.SNOW)) {
                                        miscNear = true;
                                    } else if (type.equals(StateTypes.BLACK_BED)
                                            || type.equals(StateTypes.BLUE_BED)
                                            || type.equals(StateTypes.BROWN_BED)
                                            || type.equals(StateTypes.CYAN_BED)
                                            || type.equals(StateTypes.GRAY_BED)
                                            || type.equals(StateTypes.LIGHT_GRAY_BED)
                                            || type.equals(StateTypes.LIME_BED)
                                            || type.equals(StateTypes.MAGENTA_BED)
                                            || type.equals(StateTypes.ORANGE_BED)
                                            || type.equals(StateTypes.PINK_BED)
                                            || type.equals(StateTypes.PURPLE_BED)
                                            || type.equals(StateTypes.RED_BED)
                                            || type.equals(StateTypes.WHITE_BED)
                                            || type.equals(StateTypes.YELLOW_BED)
                                            || type.equals(StateTypes.LIGHT_BLUE_BED)) {
                                        bedNear = true;
                                    }
                                } else if (blockBox.isCollided(normalBox)) {
                                    if (type == StateTypes.END_PORTAL || type == StateTypes.END_PORTAL_FRAME || type == StateTypes.NETHER_PORTAL) {
                                        inPortal = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!player.getInfo().isWorldLoaded())
                return;

            for (TrackedEntity entity : player.getInfo().getNearbyEntities()) {
                boolean isBlockEntity = (!entity.getEntityType().isInstanceOf(EntityTypes.LIVINGENTITY));

                if (!isBlockEntity) continue;


                CollisionBox entityBox = EntityData.getEntityBox(entity.getLocation(), entity);

                if (entityBox == null) continue;

                if (entityBox.isCollided(normalBox.copy().offset(0, -.2, 0)))
                    player.getInfo().setServerGround(true);

                if (entityBox.isCollided(normalBox)) {
                    collidedWithEntity = true;
                    player.getInfo().getLastEntityCollision().reset();
                }

                if (entityBox.isCollided(normalBox.copy().expand(0.25))) {
                    entityCollisionBoxes.add(entityBox);
                }

                if (entityBox.isCollided(normalBox.copy().expand(0.1, 0.1, 0.1))) {
                    nearSteppableEntity = true;
                }
            }

            //Bukkit.broadcastMessage("chigga5");
            onHalfBlock = onSlab || onStairs || miscNear || bedNear;

            if ((player.getMovement().getDeltaY() <= 0 || player.getPlayerVersion().isNewerThanOrEquals(ClientVersion.V_1_14))
                    && !onClimbable) {
                onClimbable = player.getInfo().getBlockOnTo().isPresent()
                        && BlockUtils.isClimbableBlock(player.getInfo().getBlockOnTo().get());
            }
        }
    }

    public SimpleCollisionBox getBox() {
        return new SimpleCollisionBox(player.getMovement().getTo().getLoc(), 0.6, 1.8);
    }
}
