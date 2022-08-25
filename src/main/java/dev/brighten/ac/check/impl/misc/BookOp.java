package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WAction;
import dev.brighten.ac.data.APlayer;
import lombok.val;
import org.bukkit.event.player.PlayerEditBookEvent;

@CheckData(name = "BookOp", checkId = "bookop", type = CheckType.EXPLOIT)
public class BookOp extends Check {
    public BookOp(APlayer player) {
        super(player);
    }

    WAction<PlayerEditBookEvent> bookEdit = event -> {
        val optional = event.getNewBookMeta().getPages().stream()
                .filter(string -> string.toLowerCase().contains("run_command"))
                .findFirst();
        if(optional.isPresent()) {
            vl++;
            flag("line=" + optional.get());
            event.setCancelled(true);
        }
    };
}
