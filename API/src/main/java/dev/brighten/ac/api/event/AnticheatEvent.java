package dev.brighten.ac.api.event;


import dev.brighten.ac.api.check.ECheck;
import dev.brighten.ac.api.event.result.CancelResult;
import dev.brighten.ac.api.event.result.FlagResult;
import dev.brighten.ac.api.event.result.PunishResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import java.util.List;

public interface AnticheatEvent {

    PunishResult onPunish(Player player, ECheck check, List<String> commands, boolean cancelled);

    FlagResult onFlag(Player player, ECheck check, String information, boolean cancelled);

    CancelResult onCancel(Player player, ECheck check, boolean cancelled);

    EventPriority priority();


}
