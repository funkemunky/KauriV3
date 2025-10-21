package dev.brighten.ac.api.event;


import dev.brighten.ac.api.check.ECheck;
import dev.brighten.ac.api.event.result.CancelResult;
import dev.brighten.ac.api.event.result.FlagResult;
import dev.brighten.ac.api.event.result.PunishResult;
import dev.brighten.ac.api.platform.KauriPlayer;

import java.util.List;

public interface AnticheatEvent {

    PunishResult onPunish(KauriPlayer player, ECheck check, List<String> commands, boolean cancelled);

    FlagResult onFlag(KauriPlayer player, ECheck check, String information, boolean cancelled);

    CancelResult onCancel(KauriPlayer player, ECheck check, boolean cancelled);

    EventPriority priority();


}
