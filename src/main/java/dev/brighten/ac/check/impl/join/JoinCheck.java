package dev.brighten.ac.check.impl.join;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;

@CheckData(name = "Join", type = CheckType.ORDER)
public class JoinCheck extends Check {
    public JoinCheck(APlayer player) {
        super(player);
    }

    @Action
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.broadcastMessage("Player joined server: " + getPlayer().getBukkitPlayer().getName());
    }
}
