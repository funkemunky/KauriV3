package dev.brighten.ac.utils.wrapper;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.wrapper.impl.Wrapper_18R3;
import dev.brighten.ac.utils.wrapper.impl.Wrapper_Reflection;
import org.bukkit.Material;
import org.bukkit.World;

public abstract class Wrapper {

    private static Wrapper INSTANCE;

    public static Wrapper getInstance() {
        if(INSTANCE == null) {
            switch (PacketEvents.getAPI().getServerManager().getVersion()) {
                case V1_8_9: {
                    return INSTANCE = new Wrapper_18R3();
                }
                default: {
                    return INSTANCE = new Wrapper_Reflection();
                }
            }
        }
        return INSTANCE;
    }

    public abstract float getFriction(Material material);

    public abstract Material getType(World world, double x, double y, double z);

    public abstract boolean isCollidable(Material material);
}
