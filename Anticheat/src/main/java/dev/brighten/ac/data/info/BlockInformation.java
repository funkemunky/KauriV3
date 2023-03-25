package dev.brighten.ac.data.info;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.*;

public class BlockInformation {
    private APlayer player;
    public boolean onClimbable, onSlab, onStairs, onHalfBlock, inLiquid, inLava, inWater, inWeb, onSlime, onIce,
            onSoulSand, blocksAbove, collidesVertically, bedNear, collidesHorizontally, blocksNear, inBlock, miscNear,
            collidedWithEntity, roseBush, fenceNear, inPortal, blocksBelow, pistonNear, fenceBelow, inScaffolding, inHoney,
            nearSteppableEntity;
    public final List<SimpleCollisionBox> aboveCollisions = Collections.synchronizedList(new ArrayList<>()),
            belowCollisions = Collections.synchronizedList(new ArrayList<>());
    public final List<CollisionBox> entityCollisionBoxes = new ArrayList<>();
    //Caching material
    private final Material cobweb = XMaterial.COBWEB.parseMaterial(),
            rosebush = XMaterial.ROSE_BUSH.parseMaterial(),
            scaffolding = XMaterial.SCAFFOLDING.parseMaterial(),
            honey = XMaterial.HONEY_BLOCK.parseMaterial();
    public final Map<Material, Integer> collisionMaterialCount = new EnumMap<>(Material.class);

    public BlockInformation(APlayer objectData) {
        this.player = objectData;
    }

    private SimpleCollisionBox newBox(double width, double height) {
        return new SimpleCollisionBox(player.getMovement().getTo().getLoc(), width, height);
    }

    public void runCollisionCheck() {
        if(!Anticheat.INSTANCE.isEnabled())
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

        if(dy > 10) dy = 10;
        if(dh > 10) dh = 10;

        int startX = Location.locToBlock(player.getMovement().getTo().getLoc().x - 1 - dh);
        int endX = Location.locToBlock(player.getMovement().getTo().getLoc().x + 1 + dh);
        int startY = Location.locToBlock(player.getMovement().getTo().getLoc().y - 1 - dy);
        int endY = Location.locToBlock(player.getMovement().getTo().getLoc().y + 2.82 + dy);
        int startZ = Location.locToBlock(player.getMovement().getTo().getLoc().z - 1 - dh);
        int endZ = Location.locToBlock(player.getMovement().getTo().getLoc().z + 1 + dh);

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

        inWater = MiscUtils.isInMaterialBB(player.getBukkitPlayer().getWorld(), waterBox, Materials.WATER);
        inLava = MiscUtils.isInMaterialBB(player.getBukkitPlayer().getWorld(), lavaBox, Materials.LAVA);
        inLiquid = inWater || inLava;

        player.getInfo().setWorldLoaded(true);
        player.getInfo().setLastServerGround(player.getInfo().isServerGround());
        synchronized (belowCollisions) {
            belowCollisions.clear();
        }
        synchronized (aboveCollisions) {
            aboveCollisions.clear();
        }
        final World world = player.getBukkitPlayer().getWorld();
        int it = 12 * 12;

        int xstart = Math.min(startX, endX), xend = Math.max(startX, endX);
        int zstart = Math.min(startZ, endZ), zend = Math.max(startZ, endZ);

        SimpleCollisionBox boundsForCollision = player.getMovement().getFrom().getBox() != null
                ? player.getMovement().getFrom().getBox().copy().shrink(0.001D, 0.001D, 0.001D)
                : null;

        IntVector min;
        IntVector max;

        if(boundsForCollision != null) {
            min = new IntVector((int) boundsForCollision.minX, (int) boundsForCollision.minY, (int) boundsForCollision.minZ);
            max = new IntVector((int) boundsForCollision.maxX, (int) boundsForCollision.maxY, (int) boundsForCollision.maxZ);
        } else {
            min = new IntVector(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            max = new IntVector(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        loop: {
            for (int x = xstart; x <= xend; ++x) {
                for (int z = zstart; z <= zend; ++z) {
                    for (int y = startY; y <= endY; ++y) {
                        if (it-- <= 0) {
                            break loop;
                        }

                        final Material type =
                                player.getBlockUpdateHandler().getBlock(new IntVector(x, y, z)).getType();

                        if (type != Material.AIR) {

                            IntVector vec = new IntVector(x, y, z);
                            CollisionBox blockBox = BlockData.getData(type)
                                    .getBox(player, vec, player.getPlayerVersion());

                            // Checking of within boundsForCollision
                            if(x >= min.getX() && x <= max.getX()
                                    && y >= min.getY() && y <= max.getY()
                                    && z >= min.getZ() && z <= max.getZ()) {
                                collisionMaterialCount.compute(type, (key, count) -> {
                                    if(count == null) return 1;

                                    return count + 1;
                                });
                            }

                            if(blockBox.isCollided(normalBox)) {
                                if(type.equals(cobweb))
                                    inWeb = true;
                                else if(type.equals(scaffolding)) inScaffolding = true;
                                else if(type.equals(honey)) inHoney = true;
                            }

                            if(type.equals(rosebush))
                                roseBush = true;

                            if(normalBox.copy().offset(0, 0.6f, 0).isCollided(blockBox))
                                blocksAbove = true;

                            if(normalBox.copy().expand(1, -0.0001, 1).isIntersected(blockBox))
                                blocksNear = true;

                            if(normalBox.copy().expand(0.1, 0, 0.1)
                                    .offset(0, 1,0).isCollided(blockBox)) {
                                synchronized (aboveCollisions) {
                                    blockBox.downCast(aboveCollisions);
                                }
                            }

                            if(normalBox.copy().expand(0.1, 0, 0.1)
                                    .expandMax(0, -0.4, 0).expandMin(0, -0.55, 0)
                                    .isCollided(blockBox)) {
                                synchronized (belowCollisions) {
                                    blockBox.downCast(belowCollisions);
                                }
                            }

                            if(Materials.checkFlag(type, Materials.COLLIDABLE)) {
                                SimpleCollisionBox groundBox = newBox(0.6, 0.1)
                                        .expandMax(0, -0.5, 0);

                                if(Materials.checkFlag(type, Materials.FENCE)
                                        || Materials.checkFlag(type, Materials.WALL)) {
                                    fenceBelow = true;
                                }

                                XMaterial blockMaterial = BlockUtils.getXMaterial(type);

                                if(newBox(1.4, 0).expandMin(0, -1, 0)
                                        .expand(0.3,0,0.3)
                                        .isIntersected(blockBox))
                                    blocksBelow = true;

                                if(normalBox.isIntersected(blockBox)) inBlock = true;

                                SimpleCollisionBox box = player.getMovement().getTo().getBox().copy();

                                box.expand(Math.abs(player.getMovement().getDeltaXZ() / 2) + 0.1, -0.001,
                                        Math.abs(player.getMovement().getDeltaXZ() / 2) + 0.1);
                                if (blockBox.isCollided(box))
                                    collidesHorizontally = true;

                                box = player.getMovement().getTo().getBox().copy();
                                box.expand(0, 0.1, 0);

                                if (blockBox.isCollided(box))
                                    collidesVertically = true;

                                if(groundBox.copy().expandMin(0, -0.8, 0)
                                        .isIntersected(blockBox))
                                    player.getInfo().setNearGround(true);

                                if(groundBox.copy().expandMin(0, -0.4, 0).isCollided(blockBox)) {
                                    player.getInfo().setServerGround(true);

                                    if(blockMaterial != null)
                                        switch (blockMaterial) {
                                            case ICE:
                                            case BLUE_ICE:
                                            case FROSTED_ICE:
                                            case PACKED_ICE: {
                                                if(groundBox.isCollided(blockBox))
                                                    onIce = true;
                                                break;
                                            }
                                            case SOUL_SAND: {
                                                if(groundBox.isCollided(blockBox))
                                                    onSoulSand = true;
                                                break;
                                            }
                                            case SLIME_BLOCK: {
                                                onSlime = true;
                                                break;
                                            }
                                        }
                                }
                                if(player.getMovement().getDeltaY() > 0
                                        && player.getPlayerVersion().isBelow(ProtocolVersion.V1_14)
                                        && Materials.checkFlag(type, Materials.LADDER)
                                        && normalBox.copy().expand(0.2f, 0, 0.2f)
                                        .isCollided(blockBox)) {
                                    onClimbable = true;
                                }

                                if(blockMaterial != null) {
                                    switch (blockMaterial) {
                                        case PISTON:
                                        case PISTON_HEAD:
                                        case MOVING_PISTON:
                                        case STICKY_PISTON: {
                                            if(normalBox.copy().expand(0.5, 0.5, 0.5)
                                                    .isCollided(blockBox))
                                                pistonNear = true;
                                            break;
                                        }
                                    }
                                }

                                if(groundBox.copy().expand(0.5, 0.3, 0.5).isCollided(blockBox)) {
                                    if(Materials.checkFlag(type, Materials.SLABS))
                                        onSlab = true;
                                    else
                                    if(Materials.checkFlag(type, Materials.STAIRS))
                                        onStairs = true;
                                    else if(Materials.checkFlag(type, Materials.FENCE)
                                            || Materials.checkFlag(type, Materials.WALL)) {
                                        fenceNear = true;
                                    } else if(blockMaterial != null)
                                        switch(blockMaterial) {
                                            case CAKE:
                                            case BREWING_STAND:
                                            case FLOWER_POT:
                                            case PLAYER_HEAD:
                                            case PLAYER_WALL_HEAD:
                                            case SKELETON_SKULL:
                                            case CREEPER_HEAD:
                                            case DRAGON_HEAD:
                                            case ZOMBIE_HEAD:
                                            case ZOMBIE_WALL_HEAD:
                                            case CREEPER_WALL_HEAD:
                                            case DRAGON_WALL_HEAD:
                                            case WITHER_SKELETON_SKULL:
                                            case LANTERN:
                                            case SKELETON_WALL_SKULL:
                                            case WITHER_SKELETON_WALL_SKULL:
                                            case SNOW: {
                                                miscNear = true;
                                                break;
                                            }
                                            case BLACK_BED:
                                            case BLUE_BED:
                                            case BROWN_BED:
                                            case CYAN_BED:
                                            case GRAY_BED:
                                            case GREEN_BED:
                                            case LIME_BED:
                                            case MAGENTA_BED:
                                            case ORANGE_BED:
                                            case PINK_BED:
                                            case PURPLE_BED:
                                            case RED_BED:
                                            case WHITE_BED:
                                            case YELLOW_BED:
                                            case LIGHT_BLUE_BED:
                                            case LIGHT_GRAY_BED: {
                                                bedNear = true;
                                                break;
                                            }
                                        }
                                }
                            } else if(blockBox.isCollided(normalBox)) {
                                XMaterial blockMaterial = BlockUtils.getXMaterial(type);

                                if(blockMaterial != null)
                                    switch(blockMaterial) {
                                        case END_PORTAL:
                                        case NETHER_PORTAL: {
                                            inPortal = true;
                                            break;
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }

        if(!player.getInfo().isWorldLoaded())
            return;

        for (Entity entity : player.getInfo().getNearbyEntities()) {
            boolean isBlockEntity = !(entity instanceof LivingEntity);

            if(!isBlockEntity) continue;

            CollisionBox entityBox = EntityData.getEntityBox(entity.getLocation(), entity);

            if(entityBox == null) continue;

            if(entityBox.isCollided(normalBox.copy().offset(0, -.2, 0)))
                player.getInfo().setServerGround(true);

            if(entityBox.isCollided(normalBox)) {
                collidedWithEntity = true;
                player.getInfo().getLastEntityCollision().reset();
            }

            if(entityBox.isCollided(normalBox.copy().expand(0.25))) {
                entityCollisionBoxes.add(entityBox);
            }

            if(entityBox.isCollided(normalBox.copy().expand(0.1, 0.1, 0.1))) {
                nearSteppableEntity = true;
            }
        }

        //Bukkit.broadcastMessage("chigga5");
        onHalfBlock = onSlab || onStairs || miscNear || bedNear;

        if((player.getMovement().getDeltaY() <= 0 || player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_14))
                && !onClimbable) {
            onClimbable = player.getInfo().getBlockOnTo().isPresent()
                    && BlockUtils.isClimbableBlock(player.getInfo().getBlockOnTo().get());
        }
    }

    public SimpleCollisionBox getBox() {
        return new SimpleCollisionBox(player.getMovement().getTo().getLoc(), 0.6, 1.8);
    }
}
