package dev.chickeneer.simplyvanish.api.events;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This event is only called when a player was vanished before or is intended to be vanished.<br>
 * For more details see:
 *
 * @see SimplyVanishStateEvent
 */
public class SimplyVanishAtLoginEvent extends SimplyVanishStateEvent {

    private final boolean autoVanish;

    public SimplyVanishAtLoginEvent(@NotNull Player player, boolean vanishBefore, boolean vanishAfter, boolean autoVanish) {
        super(player.getName(), player.getUniqueId(), vanishBefore, vanishAfter);
        this.autoVanish = autoVanish;
    }

    public boolean getAutoVanish() {
        return autoVanish;
    }

}
