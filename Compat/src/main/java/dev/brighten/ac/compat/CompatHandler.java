package dev.brighten.ac.compat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import dev.brighten.ac.compat.impl.CompatHandler1_13;
import dev.brighten.ac.compat.impl.CompatHandler1_8;
import dev.brighten.ac.compat.impl.CompatHandler1_9;
import org.bukkit.entity.Player;

public abstract class CompatHandler {

    public abstract boolean isRiptiding(Player player);

    public abstract boolean isGliding(Player player);

    public static CompatHandler getINSTANCE() {
        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            return new CompatHandler1_13();
        } else if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return new CompatHandler1_9();
        } else return new CompatHandler1_8();
    }
}
