package dev.brighten.ac.utils.reflections.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedConstructor;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class MinecraftReflection {
    public static WrappedClass entity = Reflections.getNMSClass("Entity");
    public static WrappedClass axisAlignedBB = Reflections.getNMSClass("AxisAlignedBB");
    public static WrappedClass block = Reflections.getNMSClass("Block");
    public static WrappedClass iBlockData, blockBase;
    public static WrappedClass world = Reflections.getNMSClass("World");
    public static WrappedClass classBlockInfo;
    public static WrappedClass minecraftServer = Reflections.getNMSClass("MinecraftServer");
    public static WrappedClass entityPlayer = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
            ? Reflections.getClass("net.minecraft.server.level.EntityPlayer")
            : Reflections.getNMSClass("EntityPlayer");
    public static WrappedClass playerConnection = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
            ? Reflections.getClass("net.minecraft.server.network.PlayerConnection")
            : Reflections.getNMSClass("PlayerConnection");

    private static final WrappedField aBB = axisAlignedBB.getFieldByName("a");
    private static final WrappedField bBB = axisAlignedBB.getFieldByName("b");
    private static final WrappedField cBB = axisAlignedBB.getFieldByName("c");
    private static final WrappedField dBB = axisAlignedBB.getFieldByName("d");
    private static final WrappedField eBB = axisAlignedBB.getFieldByName("e");
    private static final WrappedField fBB = axisAlignedBB.getFieldByName("f");
    private static WrappedConstructor aabbConstructor;
    private static WrappedMethod idioticOldStaticConstructorAABB;
    private static final WrappedField entitySimpleCollisionBox = entity.getFirstFieldByType(axisAlignedBB.getParent());

    //Blocks
    public static WrappedClass blockPos;


    //Entity Player fields
    private static final WrappedField connectionField = entityPlayer
            .getFieldByType(playerConnection.getParent(), 0);

    //1.7 field is boundingBox
    //1.8+ method is getBoundingBox.
    public static <T> T getEntityBoundingBox(Entity entity) {
        Object vanillaEntity = CraftReflection.getEntity(entity);

        return entitySimpleCollisionBox.get(vanillaEntity);
    }

    //Can either use Player or EntityPlayer object.
    public static <T> T getPlayerConnection(Object player) {
        Object entityPlayer;
        if(player instanceof Player) {
            entityPlayer = CraftReflection.getEntityPlayer((Player)player);
        } else entityPlayer = player;

        return connectionField.get(entityPlayer);
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

    static {
        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_7_10)) {
            iBlockData = Reflections.getNMSClass("IBlockData");
            blockPos = Reflections.getNMSClass("BlockPosition");
            aabbConstructor = axisAlignedBB
                    .getConstructor(double.class, double.class, double.class, double.class, double.class, double.class);
        } else {
            idioticOldStaticConstructorAABB = axisAlignedBB.getMethod("a",
                    double.class, double.class, double.class, double.class, double.class, double.class);
        }
        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            classBlockInfo = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16)
                    ? Reflections.getNMSClass("BlockBase$Info") : Reflections.getNMSClass("Block$Info");
        }

        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16)) {
            blockBase = Reflections.getNMSClass("BlockBase");
        }
    }
}
