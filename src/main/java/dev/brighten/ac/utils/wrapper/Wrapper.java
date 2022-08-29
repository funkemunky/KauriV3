package dev.brighten.ac.utils.wrapper;

import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.wrapper.impl.Wrapper_18R3;
import dev.brighten.ac.utils.wrapper.impl.Wrapper_Reflection;
import org.bukkit.Material;

public abstract class Wrapper {

    private static Wrapper INSTANCE;

    public static Wrapper getInstance() {
        if(INSTANCE == null) {
            switch (ProtocolVersion.getGameVersion()) {
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
}
