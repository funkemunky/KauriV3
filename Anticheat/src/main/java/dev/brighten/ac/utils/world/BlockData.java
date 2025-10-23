package dev.brighten.ac.utils.world;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.utils.BlockUtils;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.brighten.ac.utils.world.blocks.*;
import dev.brighten.ac.utils.world.types.*;

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
        if (block.getBlockState().getType() == com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.FIRE && boxes.isNull())
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

    ,_WALL(new DynamicWall(), BlockTags.WALLS.getStates().toArray(new StateType[0])),

    _SKULL(new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F),
            StateTypes.CREEPER_HEAD, StateTypes.ZOMBIE_HEAD, StateTypes.DRAGON_HEAD, StateTypes.PLAYER_HEAD,
            StateTypes.SKELETON_SKULL, StateTypes.WITHER_SKELETON_SKULL, StateTypes.HEAVY_CORE),
    _PIGLIN_SKULL(new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D),
            StateTypes.PIGLIN_HEAD),
    _WALL_SKULL((protocol, player, b) -> switch (b.getBlockState().getFacing()) {
        case SOUTH -> new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
        case WEST -> new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
        case EAST -> new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
        default -> new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
    }, StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD, StateTypes.ZOMBIE_WALL_HEAD,
            StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL),
    _PIGLIN_WALL_SKULL((protocol, player, b) -> switch (b.getBlockState().getFacing()) {
        case SOUTH -> new HexCollisionBox(3.0D, 4.0D, 0.0D, 13.0D, 12.0D, 8.0D);
        case EAST -> new HexCollisionBox(0.0D, 4.0D, 3.0D, 8.0D, 12.0D, 13.0D);
        case WEST -> new HexCollisionBox(8.0D, 4.0D, 3.0D, 16.0D, 12.0D, 13.0D);
        default -> new HexCollisionBox(3.0D, 4.0D, 8.0D, 13.0D, 12.0D, 16.0D);
    }, StateTypes.PIGLIN_WALL_HEAD),

    _DOOR(new DoorHandler(), BlockTags.DOORS.getStates().toArray(new StateType[0])),

    _HOPPER(new HopperBounding(), StateTypes.HOPPER),
    _CAKE((protocol, player, block) -> {
        double f1 = (1 + block.getBlockState().getBites() * 2) / 16D;

        return new SimpleCollisionBox(f1, 0, 0.0625, 1 - 0.0625, 0.5, 1 - 0.0625);
    }, StateTypes.CAKE),
    _COCOA_BEAN((protocol, player, block) -> {
        int age = block.getBlockState().getAge();
        if (protocol.isNewerThanOrEquals(ClientVersion.V_1_9_1) && protocol.isOlderThan(ClientVersion.V_1_11))
            age = Math.min(age, 1);

        switch (block.getBlockState().getFacing()) {
            case EAST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(11.0D, 7.0D, 6.0D, 15.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(9.0D, 5.0D, 5.0D, 15.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(7.0D, 3.0D, 4.0D, 15.0D, 12.0D, 12.0D);
                }
            case WEST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(1.0D, 7.0D, 6.0D, 5.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(1.0D, 5.0D, 5.0D, 7.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(1.0D, 3.0D, 4.0D, 9.0D, 12.0D, 12.0D);
                }
            case NORTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 1.0D, 10.0D, 12.0D, 5.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 1.0D, 11.0D, 12.0D, 7.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 1.0D, 12.0D, 12.0D, 9.0D);
                }
            case SOUTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 11.0D, 10.0D, 12.0D, 15.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 9.0D, 11.0D, 12.0D, 15.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 7.0D, 12.0D, 12.0D, 15.0D);
                }
        }
        return NoCollisionBox.INSTANCE;
    }, StateTypes.COCOA),
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
    }, BlockTags.FENCE_GATES.getStates().toArray(new StateType[0])),

    _FENCE(new DynamicFence(), BlockTags.FENCES.getStates().toArray(new StateType[0])),
    _PANE(new DynamicPane(), StateTypes.values().stream()
            .filter(s -> s.getName().endsWith("PANE"))
            .toArray(StateType[]::new)),


    _SNOW((protocol, player, b) -> {
        int height = b.getBlockState().getLayers();
        if(height <= 0) {
            player.getBukkitPlayer().sendMessage(String.format("Height is %s for snow block at ", height) + b.getLocation());
        }
        if (height == 0) return new SimpleCollisionBox(0, 0, 0, 1, 0, 1); // return NoCollisionBox.INSTANCE;
        return new SimpleCollisionBox(0, 0, 0, 1, (height - 1) * 0.125, 1);
    }, StateTypes.SNOW),

    _SLAB((protocol, player, b) -> {
        Type slabType = b.getBlockState().getTypeData();
        if (slabType == Type.DOUBLE) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        } else if (slabType == Type.BOTTOM) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
    }, BlockTags.SLABS.getStates().toArray(new StateType[0])),

    _STAIR(new DynamicStair(), StateTypes.values().stream().filter(mat -> mat.getName().toUpperCase().contains("STAIRS"))
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
            BlockTags.WOOL_CARPETS.getStates().toArray(new StateType[0])),
    _Daylight(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            StateTypes.DAYLIGHT_DETECTOR),
    _LILIPAD((v, player, b) -> {
        if (v.isOlderThan(ClientVersion.V_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
        return new SimpleCollisionBox(0.0625, 0.0F, 0.0625, 0.9375, 0.015625F, 0.9375);
    }, StateTypes.LILY_PAD),

    _BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            StateTypes.values().stream().filter(mat -> mat.getName().toUpperCase().contains("BED") && !mat.getName().toUpperCase().contains("ROCK"))
                    .toArray(StateType[]::new)),

    _WALL_SIGN((protocol, player, block) -> switch (block.getBlockState().getFacing()) {
        case NORTH, SOUTH -> new HexCollisionBox(0.0, 14.0, 6.0, 16.0, 16.0, 10.0);
        case WEST, EAST -> new HexCollisionBox(6.0, 14.0, 0.0, 10.0, 16.0, 16.0);
        default -> NoCollisionBox.INSTANCE;
    }, BlockTags.WALL_HANGING_SIGNS.getStates().toArray(new StateType[0])),

    _TRAPDOOR(new TrapDoorHandler(), StateTypes.values().stream()
            .filter(mat -> mat.getName().toUpperCase().contains("TRAP_DOOR")
                    || mat.getName().toUpperCase().contains("TRAPDOOR")).toArray(StateType[]::new)),

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

    _NONE(NoCollisionBox.INSTANCE, Stream.of(StateTypes.TORCH, StateTypes.REDSTONE_TORCH,
                    StateTypes.REDSTONE_WIRE, StateTypes.REDSTONE_WALL_TORCH, StateTypes.POWERED_RAIL, StateTypes.WALL_TORCH,
            StateTypes.RAIL, StateTypes.ACTIVATOR_RAIL, StateTypes.DETECTOR_RAIL, StateTypes.AIR, StateTypes.FERN,
            StateTypes.TRIPWIRE, StateTypes.TRIPWIRE_HOOK, StateTypes.TWISTING_VINES_PLANT, StateTypes.WEEPING_VINES_PLANT,
                    StateTypes.TWISTING_VINES, StateTypes.WEEPING_VINES, StateTypes.CAVE_VINES, StateTypes.CAVE_VINES_PLANT,
                    StateTypes.TALL_SEAGRASS, StateTypes.SEAGRASS, StateTypes.SHORT_GRASS, StateTypes.FERN, StateTypes.NETHER_SPROUTS,
                    StateTypes.DEAD_BUSH, StateTypes.SUGAR_CANE, StateTypes.SWEET_BERRY_BUSH, StateTypes.WARPED_ROOTS,
                    StateTypes.CRIMSON_ROOTS, StateTypes.TORCHFLOWER_CROP, StateTypes.PINK_PETALS, StateTypes.TALL_GRASS,
                    StateTypes.LARGE_FERN, StateTypes.BAMBOO_SAPLING, StateTypes.HANGING_ROOTS,
                    StateTypes.SMALL_DRIPLEAF, StateTypes.END_PORTAL, StateTypes.LEVER, StateTypes.PUMPKIN_STEM, StateTypes.MELON_STEM,
                    StateTypes.ATTACHED_MELON_STEM, StateTypes.ATTACHED_PUMPKIN_STEM, StateTypes.BEETROOTS, StateTypes.POTATOES,
                    StateTypes.WHEAT, StateTypes.CARROTS, StateTypes.NETHER_WART, StateTypes.MOVING_PISTON, StateTypes.AIR, StateTypes.CAVE_AIR,
                    StateTypes.VOID_AIR, StateTypes.LIGHT, StateTypes.WATER)
            .toArray(StateType[]::new)),

    _NONE2(NoCollisionBox.INSTANCE, StateTypes.values().stream()
            .filter(state -> state.getName().toUpperCase().contains("PLATE")
                    || !(state.isSolid()
                    || state == StateTypes.LAVA
                    || state == StateTypes.SCAFFOLDING
                    || state == StateTypes.PITCHER_CROP
                    || state == StateTypes.HEAVY_CORE
                    || state == StateTypes.PALE_MOSS_CARPET || BlockTags.WALL_HANGING_SIGNS.contains(state)))
            .toArray(StateType[]::new));

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

    public CollisionBox getDefaultBox() {
        if (this.box != null) {
            return this.box.copy();
        } else {
            return new DynamicCollisionBox(dynamic, null, null,
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
        }
    }

    public CollisionBox getBox(APlayer player, WrappedBlock block, ClientVersion version) {
        return getBox(player, block.getLocation(), version);
    }

    public CollisionBox getBox(APlayer player, Vector3i block, ClientVersion version) {
        if (this.box != null)
            return this.box.copy().offset(block.getX(), block.getY(), block.getZ());
        return new DynamicCollisionBox(dynamic, player, player.getBlockUpdateHandler().getBlock(block), version)
                .offset(block.getX(), block.getY(), block.getZ());
    }

    private static final Map<StateType, BlockData> lookup = new HashMap<>();

    static {
        for (BlockData data : values()) {
            for (StateType state : data.materials) {
                lookup.put(state, data);
            }
        }
    }

    public static BlockData getData(StateType state) {
        BlockData data = lookup.get(state);
        return data != null ? data : _DEFAULT;
    }
}