package dev.brighten.ac.utils.world;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.blocks.*;
import dev.brighten.ac.utils.world.types.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;
import java.util.stream.Stream;

public enum BlockData {
    _DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            XMaterial.STONE.parseMaterial()),
    _VINE((v, protocol, block) -> {
        byte data = block.getMaterialData().getData();

        if((data & 4) == 4)
            return new SimpleCollisionBox(0., 0., 0.,
                    1., 1., 0.0625);

        if((data & 8) == 8)
            return new SimpleCollisionBox(0.9375, 0., 0.,
                    1., 1., 1.);

        if((data & 1) == 1)
            return new SimpleCollisionBox(0., 0., 0.9375,
                    1., 1., 1.);

        if((data & 2) == 2)
            return new SimpleCollisionBox(0., 0., 0.,
                    0.0625, 1., 1.);

        return new SimpleCollisionBox(0,0,0,1.,1.,1.);
    }, XMaterial.VINE.parseMaterial()),

    _LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            XMaterial.WATER.parseMaterial(), XMaterial.LAVA.parseMaterial(),
            MiscUtils.match("STATIONARY_LAVA"), MiscUtils.match("STATIONARY_WATER")),

    _BREWINGSTAND(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),                      //base
            new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625) //top
    ), Material.BREWING_STAND),

   /* _RAIL((protocol, player, b) -> ReflectionsUtil.getBlockBoundingBox(BlockUtils.getBlock(b.getLocation()))
            .toCollisionBox(),Arrays.stream(Material.values())
            .filter(mat -> mat.name().toLowerCase().contains("rail"))
            .toArray(Material[]::new)),*/

    _ANVIL((protocol, player, b) -> {
        BlockFace face = b.getBlockState().getFacing();
        if (protocol.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));
            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
            } else {
                complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
            }

            return complexAnvil;
        } else {
            // Just a single solid collision box with 1.12
            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial())

    ,_WALL(new DynamicWall(), Arrays.stream(XMaterial.values())
            .filter(mat -> mat.name().contains("WALL"))
            .map(BlockData::m)
            .toArray(Material[]::new)),

    _SKULL((protocol, player, b) -> {
        BlockFace face = b.getBlockState().getFacing();
        return switch (face) {
            case EAST -> new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
            case NORTH -> new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
            case SOUTH -> new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
            case DOWN -> new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            default -> new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F); //WEST
        };
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("SKULL")
            || mat.name().contains("HEAD")).toArray(Material[]::new)),

    _DOOR(new DoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> !mat.name().contains("TRAP") && !mat.name().contains("ITEM")
                    && mat.name().contains("DOOR")
                    // Potential cause for ClassCastException to MaterialData instead of Door
                    && !mat.name().equals("WOOD_DOOR") && !mat.name().equals("IRON_DOOR"))
            .toArray(Material[]::new)),

    _HOPPER(new HopperBounding(), XMaterial.HOPPER.parseMaterial()),
    _CAKE((protocol, player, block) -> {
        double f1 = (1 + block.getBlockState().getBites() * 2) / 16D;

        return new SimpleCollisionBox(f1, 0, 0.0625, 1 - 0.0625, 0.5, 1 - 0.0625);
    }, Arrays.stream(Material.values()).filter(m -> m.name().contains("CAKE")).toArray(Material[]::new)),

    _LADDER((protocol, player, b) -> {
        float var3 = 0.125F;
        BlockFace facing = b.getBlockState().getFacing();

        return switch (facing) {
            case NORTH -> new SimpleCollisionBox(0.0F, 0.0F, 1.0F - var3, 1.0F, 1.0F, 1.0F);
            case SOUTH -> new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var3);
            case WEST -> new SimpleCollisionBox(1.0F - var3, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            default -> new SimpleCollisionBox(0.0F, 0.0F, 0.0F, var3, 1.0F, 1.0F);
        };
    }, XMaterial.LADDER.parseMaterial()),

    _FENCE_GATE((protocol, player, b) -> {
        if (b.getBlockState().isOpen())
            return NoCollisionBox.INSTANCE;

        return switch (b.getBlockState().getFacing()) {
            case NORTH, SOUTH ->
                    new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
            case WEST, EAST ->
                    new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
            default -> NoCollisionBox.INSTANCE;
        };
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    _FENCE(new DynamicFence(), Arrays.stream(XMaterial.values())
            .filter(mat -> mat.name().equals("FENCE") || mat.name().endsWith("FENCE"))
            .map(BlockData::m)
            .toArray(Material[]::new)),
    _PANE(new DynamicPane(), MiscUtils.match("THIN_GLASS"), MiscUtils.match("STAINED_GLASS_PANE"),
            MiscUtils.match("IRON_FENCE"), MiscUtils.match("IRON_BARS")),


    _SNOW((protocol, player, b) -> {
        int height = b.getBlockState().getLayers();
        if (height == 0) return new SimpleCollisionBox(0, 0, 0, 1, 0, 1); // return NoCollisionBox.INSTANCE;
        return new SimpleCollisionBox(0, 0, 0, 1, height * 0.125, 1);
    }, XMaterial.SNOW.parseMaterial()),

    _SLAB((protocol, player, b) -> {
        Type slabType = b.getBlockState().getTypeData();
        if (slabType == Type.DOUBLE) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        } else if (slabType == Type.BOTTOM) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
    }, Arrays.stream(Material.values()).filter(mat ->
            mat.name().contains("STEP") || mat.name().contains("SLAB"))
            .filter(mat -> !mat.name().contains("DOUBLE"))
            .toArray(Material[]::new)),

    _STAIR(new DynamicStair(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("STAIRS"))
            .map(BlockData::m)
            .toArray(Material[]::new)),

    _CHEST((protocol, player, b) -> {
        if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.NORTH)
                .map(block -> block.getType().name().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0F,
                    0.9375F, 0.875F, 0.9375F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.SOUTH)
                .map(block -> block.getType().name().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
                    0.9375F, 0.875F, 1.0F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.WEST)
                .map(block -> block.getType().name().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0625F,
                    0.9375F, 0.875F, 0.9375F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.EAST)
                .map(block -> block.getType().name().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
                    1.0F, 0.875F, 0.9375F);
        } else {
            return new SimpleCollisionBox(
                    0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
    },
            XMaterial.CHEST.parseMaterial(), 
            XMaterial.TRAPPED_CHEST.parseMaterial()),
    _ENDERCHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F),
            XMaterial.ENDER_CHEST.parseMaterial()),
    _ETABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1),
            MiscUtils.match("ENCHANTMENT_TABLE")),
    _FRAME(new SimpleCollisionBox(0, 0, 0, 1, 1 - (0.0625 * 3), 1),
            MiscUtils.match("ENDER_PORTAL_FRAME")),

    _CARPET(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F),
            Arrays.stream(Material.values()).filter(m -> m.name().contains("CARPET")).toArray(Material[]::new)),
    _Daylight(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            MiscUtils.match("DAYLIGHT_DETECTOR"), MiscUtils.match("DAYLIGHT_DETECTOR_INVERTED")),
    _LILIPAD((v, player, b) -> {
        if (v.isOlderThan(ClientVersion.V_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
        return new SimpleCollisionBox(0.0625, 0.0F, 0.0625, 0.9375, 0.015625F, 0.9375);
    }, XMaterial.LILY_PAD.parseMaterial()),

    _BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK"))
                    .map(BlockData::m)
                    .toArray(Material[]::new)),


    _TRAPDOOR(new TrapDoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR")
                    || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),

    _STUPID(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F),
            MiscUtils.match("DIODE_BLOCK_OFF"), MiscUtils.match("DIODE_BLOCK_ON"),
            MiscUtils.match("REDSTONE_COMPARATOR_ON"), MiscUtils.match("REDSTONE_COMPARATOR_OFF")),

    _STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375, 
            0.625, 0.625, 0.625),
            XMaterial.STRUCTURE_VOID.parseMaterial()),
    
    _END_ROD(new DynamicRod(), XMaterial.END_ROD.parseMaterial()),
    _CAULDRON(new CouldronBounding(), Material.CAULDRON),
    _CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625, 
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625), XMaterial.CACTUS.parseMaterial()),
    _PISTON_BASE(new PistonBaseCollision(), m(XMaterial.PISTON), m(XMaterial.STICKY_PISTON)),

    _PISTON_ARM(new PistonDickCollision(), m(XMaterial.PISTON_HEAD)),

    _SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1),
            XMaterial.SOUL_SAND.parseMaterial()),
    _CAMPFIRE((version, player, block) -> version.isNewerThan(ClientVersion.V_1_14)
            ? new SimpleCollisionBox(0,0,0, 1, 0.4375, 1)
            : NoCollisionBox.INSTANCE, XMaterial.CAMPFIRE.parseMaterial()),
    _LECTERN((version, player, block) -> {
        if(version.isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new ComplexCollisionBox(
                    new SimpleCollisionBox(0, 0.9375, 0, 1, 0.9375, 1),
                    new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),
                    new SimpleCollisionBox(0.25, 0.125, 0.25, 0.75, 0.875, 0.75));
        } else return NoCollisionBox.INSTANCE;
    }, XMaterial.LECTERN.parseMaterial()),
    _POT(new SimpleCollisionBox(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875),
            Material.FLOWER_POT),

    _WALL_SIGN((version, player, block) -> {

        double var4 = 0.28125;
        double var5 = 0.78125;
        double var6 = 0;
        double var7 = 1.0;
        double var8 = 0.125;

        return switch (block.getBlockState().getFacing()) {
            case NORTH -> new SimpleCollisionBox(var6, var4, 1.0 - var8, var7, var5, 1.0);
            case SOUTH -> new SimpleCollisionBox(var6, var4, 0.0, var7, var5, var8);
            case WEST -> new SimpleCollisionBox(1.0 - var8, var4, var6, 1.0, var5, var7);
            case EAST -> new SimpleCollisionBox(0.0, var4, var6, var8, var5, var7);
            default -> new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        };
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_SIGN"))
            .toArray(Material[]::new)),

    _SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            Arrays.stream(Material.values()).filter(m -> m.name().endsWith("_SIGN") || m.name().startsWith("SIGN"))
                    .toArray(Material[]::new)),
    _BUTTON((version, player, block) -> {
        BlockFace face = block.getBlockState().getFacing();

        face = face.getOppositeFace();
        boolean flag = block.getBlockState().isPowered(); //is powered;
        double f2 = (float)(flag ? 1 : 2) / 16.0;
        return switch (face) {
            case EAST -> new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875);
            case WEST -> new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875);
            case SOUTH -> new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2);
            case NORTH -> new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0);
            case UP -> new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625);
            case DOWN -> new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625);
            default -> NoCollisionBox.INSTANCE;
        };
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),
    
    _LEVER((version, player, block) -> {
        BlockFace face = block.getBlockState().getFacing();

        double f = 0.1875;
        return switch (face) {
            case EAST -> new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f);
            case WEST -> new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f);
            case SOUTH -> new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0);
            case NORTH -> new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0);
            case UP -> new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75);
            case DOWN -> new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75);
            default -> NoCollisionBox.INSTANCE;
        };
    }, XMaterial.LEVER.parseMaterial()),

    _NONE(NoCollisionBox.INSTANCE, Stream.of(XMaterial.TORCH, XMaterial.REDSTONE_TORCH,
            XMaterial.REDSTONE_WIRE, XMaterial.REDSTONE_WALL_TORCH, XMaterial.POWERED_RAIL, XMaterial.WALL_TORCH,
            XMaterial.RAIL, XMaterial.ACTIVATOR_RAIL, XMaterial.DETECTOR_RAIL, XMaterial.AIR, XMaterial.FERN,
            XMaterial.TRIPWIRE, XMaterial.TRIPWIRE_HOOK)
            .map(BlockData::m)
            .toArray(Material[]::new)),

    _NONE2(NoCollisionBox.INSTANCE, Arrays.stream(XMaterial.values())
            .filter(mat -> {
                List<String> names = new ArrayList<>(Arrays.asList(mat.getLegacy()));
                names.add(mat.name());
                return names.stream().anyMatch(name ->
                        name.contains("PLATE"));
            }).map(BlockData::m).toArray(Material[]::new));

    private CollisionBox box;
    private CollisionFactory dynamic;
    private final Material[] materials;

    BlockData(CollisionBox box, Material... materials) {
        this.box = box;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    BlockData(CollisionFactory dynamic, Material... materials) {
        this.dynamic = dynamic;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public CollisionBox getBox(Block block, ClientVersion version) {
        if (this.box != null)
            return this.box.copy().offset(block.getX(), block.getY(), block.getZ());
        return new DynamicCollisionBox(dynamic, null, new WrappedBlock(new IntVector(block.getLocation()),
                block.getType(),
                SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData())),
                version)
                .offset(block.getX(), block.getY(), block.getZ());
    }

    public CollisionBox getBox(WrappedBlock block, ClientVersion version) {
        if (this.box != null)
            return this.box.copy().offset(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
        return new DynamicCollisionBox(dynamic, null, block, version)
                .offset(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
    }

    public CollisionBox getBox(APlayer player, IntVector block, ClientVersion version) {
        if (this.box != null)
            return this.box.copy().offset(block.getX(), block.getY(), block.getZ());
        return new DynamicCollisionBox(dynamic, null, player.getBlockUpdateHandler().getBlock(block), version)
                .offset(block.getX(), block.getY(), block.getZ());
    }

    private static final BlockData[] lookup = new BlockData[Material.values().length];

    static {
        for (BlockData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    public static BlockData getData(Material material) {
        Material matched = PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_13)
                ? MiscUtils.match(material.toString()) : material;
        BlockData data = lookup[matched.ordinal()];
        return data != null ? data : _DEFAULT;
    }

    private static Material m(XMaterial xmat) {
        return xmat.parseMaterial();
    }
}