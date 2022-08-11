package dev.brighten.ac.packet.handler;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class HandlerAbstract{
    static WrappedClass classNetworkManager = Reflections.getNMSClass("NetworkManager");
    static WrappedField
            fieldNetworkManager = Reflections.getNMSClass("PlayerConnection").getFieldByName("networkManager"),
            fieldPlayerConnection = Reflections.getNMSClass("EntityPlayer").getFieldByName("playerConnection");

    static String handlerName = "brighten-ac-packets";

    @Getter
    private static HandlerAbstract handler;

    public static void init() {
        if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_8)) {
            handler = new ModernHandler();
        } else handler = new LegacyHandler();

        Bukkit.getOnlinePlayers().forEach(handler::add);
    }

    public abstract void add(Player player);

    public abstract void remove(Player player);

    public abstract void sendPacket(Player player, Object packet);

    public abstract void sendPacket(APlayer player, Object packet);

    public abstract int getProtocolVersion(Player player);
}
