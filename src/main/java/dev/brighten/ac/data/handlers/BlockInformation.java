package dev.brighten.ac.data.handlers;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.EntityData;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class BlockInformation {
    private APlayer player;
    public boolean onClimbable, onSlab, onStairs, onHalfBlock, inLiquid, inLava, inWater, inWeb, onSlime, onIce,
            onSoulSand, blocksAbove, collidesVertically, bedNear, collidesHorizontally, blocksNear, inBlock, miscNear,
            collidedWithEntity, roseBush, inPortal, blocksBelow, pistonNear, fenceBelow, inScaffolding, inHoney;
    public float currentFriction, fromFriction;
    public CollisionHandler
            handler = new CollisionHandler(new ArrayList<>(), new ArrayList<>(), new KLocation(0,0,0), null);
    public final List<SimpleCollisionBox> aboveCollisions = Collections.synchronizedList(new ArrayList<>()),
            belowCollisions = Collections.synchronizedList(new ArrayList<>());
    public final List<Block> blocks = Collections.synchronizedList(new ArrayList<>());
    private static EnumMap<Material, XMaterial> matchMaterial = new EnumMap<>(Material.class);
    //Caching material
    private final Material cobweb = XMaterial.COBWEB.parseMaterial(),
            rosebush = XMaterial.ROSE_BUSH.parseMaterial(),
            scaffolding = XMaterial.SCAFFOLDING.parseMaterial(),
            honey = XMaterial.HONEY_BLOCK.parseMaterial();

    static {
        for (Material mat : Material.values()) {
            matchMaterial.put(mat, XMaterial.matchXMaterial(mat));
        }
    }

    public static XMaterial getXMaterial(Material material) {
        return matchMaterial.getOrDefault(material, null);
    }

    public BlockInformation(APlayer objectData) {
        this.player = objectData;
    }

    public void runCollisionCheck() {
        if(!Anticheat.INSTANCE.isEnabled())
            return;

        double dy = player.getMovement().getDeltaY();
        double dh = player.getMovement().getDeltaXZ();

        blocks.clear();

        player.getInfo().setServerGround(false);
        player.getInfo().setNearGround(false);
        onClimbable = fenceBelow
                = inScaffolding = inHoney
                = onSlab = onStairs = onHalfBlock = inLiquid = inLava = inWater = inWeb = onSlime = pistonNear
                = onIce = onSoulSand = blocksAbove = collidesVertically = bedNear = collidesHorizontally =
                blocksNear = inBlock = miscNear = collidedWithEntity = blocksBelow = inPortal = false;

        if(dy > 10) dy = 10;
        else if(dy < -10) dy = -10;
        if(dh > 10) dh = 10;

        int startX = Location.locToBlock(player.getMovement().getTo().getLoc().x - 1 - dh);
        int endX = Location.locToBlock(player.getMovement().getTo().getLoc().x + 1 + dh);
        int startY = Location.locToBlock(player.getMovement().getTo().getLoc().y - Math.max(0.6, 0.6 + Math.abs(dy)));
        int endY = Location.locToBlock(player.getMovement().getTo().getLoc().y + Math.max(2.1, 2.1 + Math.abs(dy)));
        int startZ = Location.locToBlock(player.getMovement().getTo().getLoc().z - 1 - dh);
        int endZ = Location.locToBlock(player.getMovement().getTo().getLoc().z + 1 + dh);

        SimpleCollisionBox waterBox = player.getMovement().getTo().getBox().copy().expand(0, -.38, 0);

        waterBox.xMin = MathHelper.floor_double(waterBox.xMin);
        waterBox.yMin = MathHelper.floor_double(waterBox.yMin);
        waterBox.zMin = MathHelper.floor_double(waterBox.zMin);
        waterBox.xMax = MathHelper.floor_double(waterBox.xMax + 1.);
        waterBox.yMax = MathHelper.floor_double(waterBox.yMax + 1.);
        waterBox.zMax = MathHelper.floor_double(waterBox.zMax + 1.);

        SimpleCollisionBox lavaBox = player.getMovement().getTo().getBox().copy().expand(-.1f, -.4f, -.1f);

        lavaBox.xMin = MathHelper.floor_double(waterBox.xMin);
        lavaBox.yMin = MathHelper.floor_double(waterBox.yMin);
        lavaBox.zMin = MathHelper.floor_double(waterBox.zMin);
        lavaBox.xMax = MathHelper.floor_double(waterBox.xMax + 1.);
        lavaBox.yMax = MathHelper.floor_double(waterBox.yMax + 1.);
        lavaBox.zMax = MathHelper.floor_double(waterBox.zMax + 1.);

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
        int it = 9 * 9;
        start:
        for (int chunkx = startX >> 4; chunkx <= endX >> 4; ++chunkx) {
            int cx = chunkx << 4;

            for (int chunkz = startZ >> 4; chunkz <= endZ >> 4; ++chunkz) {
                if (!world.isChunkLoaded(chunkx, chunkz)) {
                    player.getInfo().setWorldLoaded(false);
                    continue;
                }
                Chunk chunk = world.getChunkAt(chunkx, chunkz);
                if (chunk != null) {
                    int cz = chunkz << 4;
                    int xstart = Math.max(startX, cx);
                    int xend = Math.min(endX, cx + 16);
                    int zstart = Math.max(startZ, cz);
                    int zend = Math.min(endZ, cz + 16);

                    for (int x = xstart; x <= xend; ++x) {
                        for (int z = zstart; z <= zend; ++z) {
                            for (int y = Math.max(-50, startY); y <= endY; ++y) {
                                if (it-- <= 0) {
                                    break start;
                                }
                                if(y > 400 || y < -50) continue;
                                Block block = chunk.getBlock(x & 15, y, z & 15);
                                final Material type = block.getType();
                                if (type != Material.AIR) {
                                    blocks.add(block);

                                    CollisionBox blockBox = BlockData.getData(type)
                                            .getBox(block, player.getPlayerVersion());

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

                                    if(normalBox.copy().expand(0.1, 0, 0.1).offset(0, -1, 0)
                                            .isCollided(blockBox)) {
                                        synchronized (belowCollisions) {
                                            blockBox.downCast(belowCollisions);
                                        }

                                        if(Materials.checkFlag(type, Materials.FENCE)
                                                || Materials.checkFlag(type, Materials.WALL)) {
                                            fenceBelow = true;
                                        }
                                    }

                                    if(Materials.checkFlag(type, Materials.SOLID)) {
                                        SimpleCollisionBox groundBox = normalBox.copy()
                                                .offset(0, -.49, 0).expandMax(0, -1.2, 0);

                                        XMaterial blockMaterial = getXMaterial(type);

                                        if(normalBox.copy().expand(0.4, 0, 0.4).expandMin(0, -1, 0)
                                                .isIntersected(blockBox))
                                            blocksBelow = true;

                                        if(normalBox.isIntersected(blockBox)) inBlock = true;

                                        SimpleCollisionBox box = player.getMovement().getTo().getBox().copy();

                                        box.expand(Math.abs(player.getMovement().getDeltaX()) + 0.1, -0.001,
                                                Math.abs(player.getMovement().getDeltaZ()) + 0.1);
                                        if (blockBox.isCollided(box))
                                            collidesHorizontally = true;

                                        box = player.getMovement().getTo().getBox().copy();
                                        box.expand(0, 0.1, 0);

                                        if (blockBox.isCollided(box))
                                            collidesVertically = true;

                                        if(groundBox.copy().expandMin(0, -0.8, 0).expand(0.2, 0, 0.2)
                                                .isIntersected(blockBox))
                                            player.getInfo().setNearGround(true);

                                        if(groundBox.isCollided(blockBox)) {
                                            player.getInfo().setServerGround(true);

                                            if(blockMaterial != null)
                                            switch (blockMaterial) {
                                                case ICE:
                                                case BLUE_ICE:
                                                case FROSTED_ICE:
                                                case PACKED_ICE: {
                                                    onIce = true;
                                                    break;
                                                }
                                                case SOUL_SAND: {
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
                                            else
                                            if(blockMaterial != null)
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
                                        XMaterial blockMaterial = getXMaterial(type);

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
            }
        }

        if(!player.getInfo().isWorldLoaded())
            return;

        CollisionHandler handler = new CollisionHandler(blocks,
                player.getInfo().getNearbyEntities(),
                player.getMovement().getTo().getLoc(), player);

        //Bukkit.broadcastMessage("chigga4");

        for (Entity entity : handler.getEntities()) {
            CollisionBox entityBox = EntityData.getEntityBox(entity.getLocation(), entity);

            if(entityBox == null) continue;

            if(entityBox.isCollided(normalBox.copy().offset(0, -.1, 0)))
                player.getInfo().setServerGround(true);

            if(entityBox.isCollided(normalBox))
                collidedWithEntity = true;
        }

        //Bukkit.broadcastMessage("chigga5");
        onHalfBlock = onSlab || onStairs || miscNear || bedNear;

        if((player.getMovement().getDeltaY() <= 0 || player.getPlayerVersion().isOrAbove(ProtocolVersion.V1_14))
                && !onClimbable) {
            onClimbable = player.getInfo().getBlockOnTo().isPresent()
                    && BlockUtils.isClimbableBlock(player.getInfo().getBlockOnTo().get());
        }

        handler.setSize(0.6f, 1.8f);
        handler.setOffset(0f);

        this.handler.getEntities().clear();
        this.handler = handler;
    }

    public SimpleCollisionBox getBox() {
        return new SimpleCollisionBox(player.getMovement().getTo().getLoc().toVector(), player.getMovement().getTo().getLoc().toVector())
                .expand(0.3, 0,0.3).expandMax(0, 1.8, 0);
    }
}
