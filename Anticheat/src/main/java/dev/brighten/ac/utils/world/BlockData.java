package dev.brighten.ac.utils.world;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.world.blocks.*;
import dev.brighten.ac.utils.world.types.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.block.Block;

import java.util.*;
import java.util.stream.Stream;

public enum BlockData {
    _DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            StateTypes.STONE),
    _VINE((v, protocol, block) -> {
        ComplexCollisionBox boxes = new ComplexCollisionBox();

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && block.getBlockState().isUp())
            boxes.add(new HexCollisionBox(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (block.getBlockState().getWest() == West.TRUE)
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D));

        if (block.getBlockState().getEast() == East.TRUE)
            boxes.add(new HexCollisionBox(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (block.getBlockState().getNorth() == North.TRUE)
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D));

        if (block.getBlockState().getSouth() == South.TRUE)
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D));

        // This is where fire differs from vine with its hitbox
        if (block.getBlockState().getType() == StateTypes.FIRE && boxes.isNull())
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

        return boxes;
    }, StateTypes.VINE),

    _LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            StateTypes.WATER, StateTypes.LAVA),

    _BREWINGSTAND(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),                      //base
            new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625) //top
    ), StateTypes.BREWING_STAND),

   /* _RAIL((protocol, player, b) -> ReflectionsUtil.getBlockBoundingBox(BlockUtils.getBlock(b.getLocation()))
            .toCollisionBox(),StateTypes.values().stream()
            .filter(mat -> mat.name().toLowerCase().contains("rail"))
            .toArray(StateType[]::new)),*/

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
    }, StateTypes.ANVIL, StateTypes.CHIPPED_ANVIL, StateTypes.DAMAGED_ANVIL)

    ,_WALL(new DynamicWall(), StateTypes.values().stream()
            .filter(mat -> mat.getName().contains("WALL"))
            .toArray(StateType[]::new)),

    _SKULL((protocol, player, b) -> {
        BlockFace face = b.getBlockState().getFacing();
        return switch (face) {
            case EAST -> new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
            case NORTH -> new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
            case SOUTH -> new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
            case DOWN -> new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            default -> new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F); //WEST
        };
    }, StateTypes.values().stream().filter(mat -> mat.getName().contains("SKULL")
            || mat.getName().contains("HEAD")).toArray(StateType[]::new)),

    _DOOR(new DoorHandler(), StateTypes.values().stream()
            .filter(mat -> !mat.getName().contains("TRAP") && !mat.getName().contains("ITEM")
                    && mat.getName().contains("DOOR")
                    // Potential cause for ClassCastException to MaterialData instead of Door
                    && !mat.getName().equals("WOOD_DOOR") && !mat.getName().equals("IRON_DOOR"))
            .toArray(StateType[]::new)),

    _HOPPER(new HopperBounding(), StateTypes.HOPPER),
    _CAKE((protocol, player, block) -> {
        double f1 = (1 + block.getBlockState().getBites() * 2) / 16D;

        return new SimpleCollisionBox(f1, 0, 0.0625, 1 - 0.0625, 0.5, 1 - 0.0625);
    }, StateTypes.values().stream().filter(m -> m.getName().contains("CAKE")).toArray(StateType[]::new)),

    _LADDER((protocol, player, b) -> {
        float var3 = 0.125F;
        BlockFace facing = b.getBlockState().getFacing();

        return switch (facing) {
            case NORTH -> new SimpleCollisionBox(0.0F, 0.0F, 1.0F - var3, 1.0F, 1.0F, 1.0F);
            case SOUTH -> new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, var3);
            case WEST -> new SimpleCollisionBox(1.0F - var3, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            default -> new SimpleCollisionBox(0.0F, 0.0F, 0.0F, var3, 1.0F, 1.0F);
        };
    }, StateTypes.LADDER),

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
    }, StateTypes.values().stream().filter(mat -> mat.getName().contains("FENCE") && mat.getName().contains("GATE"))
            .toArray(StateType[]::new)),

    _FENCE(new DynamicFence(), StateTypes.values().stream()
            .filter(mat -> mat.getName().equals("FENCE") || mat.getName().endsWith("FENCE"))
            .toArray(StateType[]::new)),
    _PANE(new DynamicPane(), StateTypes.values().stream()
            .filter(s -> s.getName().endsWith("PANE"))
            .toArray(StateType[]::new)),


    _SNOW((protocol, player, b) -> {
        int height = b.getBlockState().getLayers();
        if (height == 0) return new SimpleCollisionBox(0, 0, 0, 1, 0, 1); // return NoCollisionBox.INSTANCE;
        return new SimpleCollisionBox(0, 0, 0, 1, height * 0.125, 1);
    }, StateTypes.SNOW),

    _SLAB((protocol, player, b) -> {
        Type slabType = b.getBlockState().getTypeData();
        if (slabType == Type.DOUBLE) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        } else if (slabType == Type.BOTTOM) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
    }, StateTypes.values().stream().filter(mat ->
            mat.getName().contains("STEP") || mat.getName().contains("SLAB"))
            .filter(mat -> !mat.getName().contains("DOUBLE"))
            .toArray(StateType[]::new)),

    _STAIR(new DynamicStair(), StateTypes.values().stream().filter(mat -> mat.getName().contains("STAIRS"))
            .toArray(StateType[]::new)),

    _CHEST((protocol, player, b) -> {
        if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.NORTH)
                .map(block -> block.getType().getName().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0F,
                    0.9375F, 0.875F, 0.9375F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.SOUTH)
                .map(block -> block.getType().getName().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
                    0.9375F, 0.875F, 1.0F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.WEST)
                .map(block -> block.getType().getName().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0625F,
                    0.9375F, 0.875F, 0.9375F);
        } else if(BlockUtils.getRelative(player, b.getLocation(), org.bukkit.block.BlockFace.EAST)
                .map(block -> block.getType().getName().contains("CHEST"))
                .orElse(false)) {
            return new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
                    1.0F, 0.875F, 0.9375F);
        } else {
            return new SimpleCollisionBox(
                    0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }
    },
            StateTypes.CHEST,
            StateTypes.TRAPPED_CHEST),
    _ENDERCHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F),
            StateTypes.ENDER_CHEST),
    _ETABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1), StateTypes.ENCHANTING_TABLE),
    _FRAME(new SimpleCollisionBox(0, 0, 0, 1, 1 - (0.0625 * 3), 1),
            StateTypes.END_PORTAL_FRAME),

    _CARPET(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F),
            StateTypes.values().stream().filter(m -> m.getName().contains("CARPET")).toArray(StateType[]::new)),
    _Daylight(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            StateTypes.DAYLIGHT_DETECTOR),
    _LILIPAD((v, player, b) -> {
        if (v.isOlderThan(ClientVersion.V_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
        return new SimpleCollisionBox(0.0625, 0.0F, 0.0625, 0.9375, 0.015625F, 0.9375);
    }, StateTypes.LILY_PAD),

    _BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            StateTypes.values().stream().filter(mat -> mat.getName().contains("BED") && !mat.getName().contains("ROCK"))
                    .toArray(StateType[]::new)),


    _TRAPDOOR(new TrapDoorHandler(), StateTypes.values().stream()
            .filter(mat -> mat.getName().contains("TRAP_DOOR")
                    || mat.getName().contains("TRAPDOOR")).toArray(StateType[]::new)),

    _STUPID(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F),
            StateTypes.COMPARATOR),

    _STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625),
            StateTypes.STRUCTURE_VOID),

    _END_ROD(new DynamicRod(), StateTypes.END_ROD),
    _CAULDRON(new CouldronBounding(), StateTypes.CAULDRON),
    _CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625), StateTypes.CACTUS),
    _PISTON_BASE(new PistonBaseCollision(), StateTypes.PISTON, StateTypes.STICKY_PISTON),

    _PISTON_ARM(new PistonDickCollision(), StateTypes.PISTON_HEAD),

    _SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1),
            StateTypes.SOUL_SAND),
    _CAMPFIRE((version, player, block) -> version.isNewerThan(ClientVersion.V_1_14)
            ? new SimpleCollisionBox(0,0,0, 1, 0.4375, 1)
            : NoCollisionBox.INSTANCE, StateTypes.CAMPFIRE),
    _LECTERN((version, player, block) -> {
        if(version.isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new ComplexCollisionBox(
                    new SimpleCollisionBox(0, 0.9375, 0, 1, 0.9375, 1),
                    new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),
                    new SimpleCollisionBox(0.25, 0.125, 0.25, 0.75, 0.875, 0.75));
        } else return NoCollisionBox.INSTANCE;
    }, StateTypes.LECTERN),
    _POT(new SimpleCollisionBox(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875),
            StateTypes.FLOWER_POT),

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
    }, StateTypes.values().stream().filter(mat -> mat.getName().contains("WALL_SIGN"))
            .toArray(StateType[]::new)),

    _SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            StateTypes.values().stream().filter(m -> m.getName().endsWith("_SIGN") || m.getName().startsWith("SIGN"))
                    .toArray(StateType[]::new)),
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
    }, StateTypes.values().stream().filter(mat -> mat.getName().contains("BUTTON")).toArray(StateType[]::new)),

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
    }, StateTypes.LEVER),

    _NONE(NoCollisionBox.INSTANCE, Stream.of(StateTypes.TORCH, StateTypes.REDSTONE_TORCH,
                    StateTypes.REDSTONE_WIRE, StateTypes.REDSTONE_WALL_TORCH, StateTypes.POWERED_RAIL, StateTypes.WALL_TORCH,
            StateTypes.RAIL, StateTypes.ACTIVATOR_RAIL, StateTypes.DETECTOR_RAIL, StateTypes.AIR, StateTypes.FERN,
            StateTypes.TRIPWIRE, StateTypes.TRIPWIRE_HOOK)
            .toArray(StateType[]::new)),

    _NONE2(NoCollisionBox.INSTANCE, StateTypes.values().stream()
            .filter(mat -> mat.getName().contains("PLATE")).toArray(StateType[]::new));

    private CollisionBox box;
    private CollisionFactory dynamic;
    private final StateType[] materials;

    BlockData(CollisionBox box, StateType... materials) {
        this.box = box;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    BlockData(CollisionFactory dynamic, StateType... materials) {
        this.dynamic = dynamic;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    public CollisionBox getBox(Block block, ClientVersion version) {
        if (this.box != null)
            return this.box.copy().offset(block.getX(), block.getY(), block.getZ());
        var convert = SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData());
        return new DynamicCollisionBox(dynamic, null, new WrappedBlock(new IntVector(block.getLocation()),
                convert.getType(),
                convert),
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

    private static final Map<StateType, BlockData> lookup = new HashMap<>();

    static {
        for (BlockData data : values()) {
            for (StateType mat : data.materials) lookup.put(mat, data);
        }
    }

    public static BlockData getData(StateType material) {
        BlockData data = lookup.get(material);
        return data != null ? data : _DEFAULT;
    }
}