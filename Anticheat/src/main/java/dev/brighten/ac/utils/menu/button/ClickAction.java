package dev.brighten.ac.utils.menu.button;

import dev.brighten.ac.utils.menu.Menu;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * @author Missionary (missionarymc@gmail.com)
 * @since 3/2/2018
 */
@FunctionalInterface
public interface ClickAction {

    /**
     * Preforms an operation on the given argument(s)
     *
     * @param player                         The {@link Player} that has acted
     * @param buttonClickTypeInformationPair The {@link InformationPair} that contains the {@link Button} clicked, the {@link Menu}, and the {@link ClickType}
     */
    void accept(Player player, InformationPair buttonClickTypeInformationPair);

    @Getter
    @Setter
    @AllArgsConstructor
    class InformationPair {
        private Button button;
        private ClickType clickType;
        private Menu menu;
        private int index;
    }

}
