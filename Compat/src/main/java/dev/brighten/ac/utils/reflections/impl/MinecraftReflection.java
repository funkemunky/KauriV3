package dev.brighten.ac.utils.reflections.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ServerVersion;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MinecraftReflection {
    public static WrappedClass entity = Reflections.getNMSClass("Entity");
    public static WrappedClass axisAlignedBB = Reflections.getNMSClass("AxisAlignedBB");
    public static WrappedClass entityHuman = Reflections.getNMSClass("EntityHuman");
    public static WrappedClass entityLiving = Reflections.getNMSClass("EntityLiving");
    public static WrappedClass block = Reflections.getNMSClass("Block");
    public static WrappedClass iBlockData, blockBase,
            chunkProviderServer = Reflections.getNMSClass("ChunkProviderServer");
    public static WrappedClass itemClass = Reflections.getNMSClass("Item"),
            enumChatFormat = Reflections.getNMSClass("EnumChatFormat");
    public static WrappedClass world = Reflections.getNMSClass("World");
    public static WrappedClass worldServer = Reflections.getNMSClass("WorldServer");
    public static WrappedClass playerInventory = Reflections.getNMSClass("PlayerInventory");
    public static WrappedClass itemStack = Reflections.getNMSClass("ItemStack"),
            item = Reflections.getNMSClass("Item");
    public static WrappedClass chunk = Reflections.getNMSClass("Chunk");
    public static WrappedClass classBlockInfo;
    public static WrappedClass minecraftServer = Reflections.getNMSClass("MinecraftServer");
    public static WrappedClass entityPlayer = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
            ? Reflections.getClass("net.minecraft.server.level.EntityPlayer")
            : Reflections.getNMSClass("EntityPlayer");
    public static WrappedClass playerConnection = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
            ? Reflections.getClass("net.minecraft.server.network.PlayerConnection")
            : Reflections.getNMSClass("PlayerConnection");
    public static WrappedClass networkManager = Reflections.getNMSClass("NetworkManager");
    public static WrappedClass serverConnection = Reflections.getNMSClass("ServerConnection");
    public static WrappedClass gameProfile = Reflections.getUtilClass("com.mojang.authlib.GameProfile");
    private static final WrappedClass propertyMap = Reflections.getUtilClass("com.mojang.authlib.properties.PropertyMap");
    private static final WrappedClass forwardMultiMap = Reflections.getUtilClass("com.google.common.collect.ForwardingMultimap");
    public static WrappedClass iChatBaseComponent = Reflections.getNMSClass("IChatBaseComponent"),
            chatComponentText;
    public static WrappedClass vec3D = Reflections.getNMSClass("Vec3D");

    private static final WrappedMethod getProfile = CraftReflection.craftPlayer.getMethod("getProfile");
    private static final WrappedMethod methodGetServerConnection = minecraftServer
            .getMethodByType(serverConnection.getParent(), PacketEvents.getAPI().getServerManager().getVersion()
                    .isOlderThan(ServerVersion.V_1_13) ? 1 : 0);
    private static WrappedConstructor chatComponentTextConst;
    private static final WrappedMethod getProperties = gameProfile.getMethod("getProperties");
    private static final WrappedMethod removeAll = forwardMultiMap.getMethod("removeAll", Object.class);
    private static final WrappedMethod putAll = propertyMap.getMethod("putAll", Object.class, Iterable.class);
    private static final WrappedMethod worldGetType;
    //SimpleCollisionBoxes
    private static final WrappedMethod getCubes;
    private static final WrappedField aBB = axisAlignedBB.getFieldByName("a");
    private static final WrappedField bBB = axisAlignedBB.getFieldByName("b");
    private static final WrappedField cBB = axisAlignedBB.getFieldByName("c");
    private static final WrappedField dBB = axisAlignedBB.getFieldByName("d");
    private static final WrappedField eBB = axisAlignedBB.getFieldByName("e");
    private static final WrappedField fBB = axisAlignedBB.getFieldByName("f");
    private static WrappedConstructor aabbConstructor;
    private static WrappedMethod idioticOldStaticConstructorAABB, methodBlockCollisionBox;
    private static final WrappedField entitySimpleCollisionBox = entity.getFirstFieldByType(axisAlignedBB.getParent());
    public static WrappedClass enumAnimation = Reflections.getNMSClass("EnumAnimation");

    //ItemStack methods and fields
    private static WrappedMethod enumAnimationStack;
    private static final WrappedField activeItemField;
    private static final WrappedMethod getItemMethod = itemStack.getMethodByType(item.getParent(), 0);
    private static final WrappedMethod getAnimationMethod = itemClass.getMethodByType(enumAnimation.getParent(), 0);
    private static final WrappedMethod canDestroyMethod;

    //1.13+ only
    private static WrappedClass voxelShape;
    private static WrappedClass worldReader;
    private static WrappedMethod getCubesFromVoxelShape;

    private static final WrappedMethod itemStackAsBukkitCopy = CraftReflection.craftItemStack
            .getMethod("asBukkitCopy", itemStack.getParent());

    //Blocks
    private static WrappedMethod addCBoxes;
    public static WrappedClass blockPos;
    private static WrappedConstructor blockPosConstructor;
    private static WrappedMethod getBlockData, getBlock;
    private static WrappedField blockData;
    private static final WrappedField frictionFactor;
    private static final WrappedField strength;
    private static final WrappedField chunkProvider = MinecraftReflection.worldServer
            .getFieldByType(Reflections.getNMSClass(PacketEvents.getAPI().getServerManager().getVersion()
                    .isOlderThan(ServerVersion.V_1_16)
                    && PacketEvents.getAPI().getServerManager().getVersion()
                    .isOrAbove(ServerVersion.V_1_9) ? "IChunkProvider" : "ChunkProviderServer")
                    .getParent(), 0);


    //Entity Player fields
    private static final WrappedField connectionField = entityPlayer
            .getFieldByType(playerConnection.getParent(), 0);
    private static final WrappedField connectionNetworkField = playerConnection
            .getFieldByType(networkManager.getParent(), 0);
    private static final WrappedField networkChannelField = networkManager.getFieldByType(Reflections
            .getUtilClass("io.netty.channel.Channel").getParent(), 0);

    //General Fields
    private static final WrappedField primaryThread = minecraftServer.getFirstFieldByType(Thread.class);

    public static <T> T getGameProfile(Player player) {
       return getProfile.invoke(player);
    }

    //1.7 field is boundingBox
    //1.8+ method is getBoundingBox.
    public static <T> T getEntityBoundingBox(Entity entity) {
        Object vanillaEntity = CraftReflection.getEntity(entity);

        return entitySimpleCollisionBox.get(vanillaEntity);
    }

    public static <T> T getItemInUse(HumanEntity entity) {
        Object humanEntity = CraftReflection.getEntityHuman(entity);
        return activeItemField.get(humanEntity);
    }

    //Can use either a Bukkit or vanilla object
    public static <T> T getItemFromStack(Object object) {
        Object vanillaStack;
        if(object instanceof ItemStack) {
            vanillaStack = CraftReflection.getVanillaItemStack((ItemStack)object);
        } else vanillaStack = object;

        return getItemMethod.invoke(vanillaStack);
    }

    //Can use either a Bukkit or vanilla object
    public static <T> T getItemAnimation(Object object) {
        Object vanillaStack;
        if(object instanceof ItemStack) {
            vanillaStack = CraftReflection.getVanillaItemStack((ItemStack)object);
        } else vanillaStack = object;

        Object item = getItemFromStack(vanillaStack);

        return getAnimationMethod.invoke(item, vanillaStack);
    }

    public static Object getChatComponentFromText(String string) {
        return chatComponentTextConst.newInstance(string);
    }

    public static int getPing(Player player) {
        return -1;
    }

    public static <T> T getServerConnection() {
        return methodGetServerConnection.invoke(CraftReflection.getMinecraftServer());
    }

    public static <T> T getServerConnection(Object minecraftServer) {
        return methodGetServerConnection.invoke(minecraftServer);
    }

    public static Thread getMainThread(Object minecraftServer) {
        return primaryThread.get(minecraftServer);
    }

    public static Thread getMainThread() {
        return getMainThread(CraftReflection.getMinecraftServer());
    }


    //Can either use Player or EntityPlayer object.
    public static <T> T getPlayerConnection(Object player) {
        Object entityPlayer;
        if(player instanceof Player) {
            entityPlayer = CraftReflection.getEntityPlayer((Player)player);
        } else entityPlayer = player;

        return connectionField.get(entityPlayer);
    }

    //Can either use Player or EntityPlayer object.
    public static <T> T getNetworkManager(Object player) {
        return connectionNetworkField.get(getPlayerConnection(player));
    }

    //Can either use Player or EntityPlayer object.
    public static <T> T getChannel(Object player) {
        Object networkManager = getNetworkManager(player);

        return networkChannelField.get(networkManager);
    }

    //Use the netty Channel class.
    public static void disconnectChannel(Object channel) {
        new WrappedClass(channel.getClass()).getMethod("close").invoke(channel);
    }

    private static WrappedMethod fluidMethod;
    private static final WrappedMethod getFlowMethod;

    public static ItemStack toBukkitItemStack(Object vanillaItemStack) {
        return itemStackAsBukkitCopy.invoke(null, vanillaItemStack);
    }

    /**
     * Extracts AxisAlignedBB Points.
     * @param aabb AxisAlignedBB
     * @return double[6] of points.
     */
    public static double[] fromAABB(Object aabb) {
        double[] boxArray = new double[6];

        boxArray[0] = aBB.get(aabb);
        boxArray[1] = bBB.get(aabb);
        boxArray[2] = cBB.get(aabb);
        boxArray[3] = dBB.get(aabb);
        boxArray[4] = eBB.get(aabb);
        boxArray[5] = fBB.get(aabb);

        return boxArray;
    }

    /**
     * Creates a new AxisAlignedBB.
     * @return new AxisAlignedBB
     */
    public static <T> T toAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_8)) {
            return idioticOldStaticConstructorAABB
                    .invoke(null,
                            minX, minY, minZ,
                            maxX, maxY, maxZ);
        } else return aabbConstructor
                .newInstance(minX, minY, minZ,
                        maxX, maxY, maxZ);
    }

    //Either bukkit or vanilla world object can be used.
    public static <T> T getChunkProvider(Object world) {
        Object vanillaWorld;
        if(world instanceof World) {
            vanillaWorld = CraftReflection.getVanillaWorld((World)world);
        } else vanillaWorld = world;

        return chunkProvider.get(vanillaWorld);
    }

    public static <T> List<T> getVanillaChunks(World world) {
        return Arrays.stream(world.getLoadedChunks())
                .map(c -> (T) CraftReflection.getVanillaChunk(c))
                .collect(Collectors.toList());
    }

    static {
        if(PacketEvents.getAPI().getServerManager().getVersion().isAbove(ServerVersion.V_1_7_10)) {
            iBlockData = Reflections.getNMSClass("IBlockData");
            blockPos = Reflections.getNMSClass("BlockPosition");
            blockPosConstructor = blockPos.getConstructor(int.class, int.class, int.class);
            getBlock = iBlockData.getMethodByType(block.getParent(), 0);
            blockData = PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_17)
                    ? block.getFieldByType(iBlockData.getParent(), 0) :  block.getFieldByName("blockData");
            blockPosConstructor = blockPos.getConstructor(int.class, int.class, int.class);
            getBlockData = block.getMethodByType(iBlockData.getParent(), 0);
            aabbConstructor = axisAlignedBB
                    .getConstructor(double.class, double.class, double.class, double.class, double.class, double.class);
            worldGetType = worldServer.getMethodByType(iBlockData.getParent(), 0, blockPos.getParent());
        } else {
            idioticOldStaticConstructorAABB = axisAlignedBB.getMethod("a",
                    double.class, double.class, double.class, double.class, double.class, double.class);
            worldGetType = worldServer.getMethod("getType", int.class, int.class, int.class);
        }
        if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_12)) {
            getCubes = world.getMethod("a", axisAlignedBB.getParent());

            if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_8)) {
                //1.7.10 does not have the BlockPosition object yet.
                addCBoxes = block.getMethod("a", world.getParent(), int.class, int.class, int.class,
                        axisAlignedBB.getParent(), List.class, entity.getParent());
                methodBlockCollisionBox = block
                        .getMethod("a", world.getParent(), int.class, int.class, int.class);
            } else {
                addCBoxes = block.getMethod("a", world.getParent(), blockPos.getParent(), iBlockData.getParent(),
                        axisAlignedBB.getParent(), List.class, entity.getParent());
                if(PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_9)) {
                    methodBlockCollisionBox = block
                            .getMethod("a", iBlockData.getParent(), world.getParent(), blockPos.getParent());
                } else methodBlockCollisionBox = block
                        .getMethod("a", world.getParent(), blockPos.getParent(), iBlockData.getParent());
            }

            getFlowMethod = Reflections.getNMSClass("BlockFluids")
                    .getDeclaredMethodByType(vec3D.getParent(), 0);
        } else if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_13)) {
            getCubes = world.getMethod("getCubes", entity.getParent(), axisAlignedBB.getParent());
            addCBoxes = block.getMethod("a", iBlockData.getParent(), world.getParent(), blockPos.getParent(),
                    axisAlignedBB.getParent(), List.class, entity.getParent(), boolean.class);
            methodBlockCollisionBox = block
                    .getMethod("a", iBlockData.getParent(), world.getParent(), blockPos.getParent());
            getFlowMethod = Reflections.getNMSClass("BlockFluids")
                    .getDeclaredMethodByType(vec3D.getParent(), 0);
        } else {
            classBlockInfo = PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_16)
                    ? Reflections.getNMSClass("BlockBase$Info") : Reflections.getNMSClass("Block$Info");
            worldReader = Reflections.getNMSClass("IWorldReader");
            //1.13 and 1.13.1 returns just VoxelShape while 1.13.2+ returns a Stream<VoxelShape>
            getCubes = PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_18)
                    ? worldReader.getMethodByType(List.class, 0, entity.getParent(), axisAlignedBB.getParent())
                    : (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16) ?
                    worldReader.getMethod("a", entity.getParent(), axisAlignedBB.getParent(),
                            double.class, double.class, double.class)
                    : world.getMethod("c", entity.getParent(), axisAlignedBB.getParent(), Predicate.class));
            voxelShape = Reflections.getNMSClass("VoxelShape");
            getCubesFromVoxelShape = voxelShape.getMethodByType(List.class, 0);
            fluidMethod = world.getMethodByType(Reflections.getNMSClass("Fluid").getParent(), 0, blockPos.getParent());
            getFlowMethod = Reflections.getNMSClass("Fluid").getMethodByType(vec3D.getParent(), 0);

            if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_19)) {
                chatComponentText =  Reflections.getNMSClass("ChatComponentText");
                chatComponentTextConst = chatComponentText.getConstructor(String.class);
            }
        }

        if(PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_16)) {
            blockBase = Reflections.getNMSClass("BlockBase");
        }
        if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
            activeItemField = entityHuman.getFieldByType(itemStack.getParent(), 0);
        } else {
            activeItemField = entityLiving.getFieldByType(itemStack.getParent(), 0);
        }

        canDestroyMethod = PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16)
                ? playerInventory.getMethod("b",
                PacketEvents.getAPI().getServerManager().getVersion().isAbove(ServerVersion.V_1_8_9)
                        ? iBlockData.getParent() : itemClass.getParent())
                : itemStack.getMethodByType(boolean.class, 0, iBlockData.getParent());
        if(PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) {
            frictionFactor = (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16)
                    ? block : blockBase).getFieldByName("frictionFactor");
        } else frictionFactor = blockBase.getFieldByType(float.class, 1);
        strength = PacketEvents.getAPI().getServerManager().getVersion().isOrAbove(ServerVersion.V_1_17)
                ? blockBase.getFieldByType(float.class, 0)
                : (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16)
                    ? block.getFieldByName("strength") : blockBase.getFieldByName("durability"));
    }
}
