package dev.chickeneer.simplyvanish.api.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * This event is called when setVanished is used internally (on commands and API usage) or on player logins (see NOTE below), it is not called for
 * updateVanishState, thus can be bypassed, also by API calls (!).<br>
 * If this event is canceled, no state updating will be performed, to just prevent state changes use the setVisibleAfter method.<br>
 * It is not sure that this event means a state change from visible to invisible or vice-versa, it could also be a forced state update.<br>
 * NOTE: This could also be a SimplyVanishAtLoginEvent which extends SimplyVanishStateEvent.
 */
public class SimplyVanishStateEvent extends Event implements SimplyVanishEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String playerName;
    private final UUID uuid;
    private final boolean vanishBefore;

    private boolean cancelled = false;

    private boolean vanishAfter;

    public SimplyVanishStateEvent(@NotNull String name, @Nullable UUID uuid, boolean vanishBefore, boolean vanishAfter) {
        this.playerName = name;
        this.uuid = uuid;
        this.vanishBefore = vanishBefore;
        this.vanishAfter = vanishAfter;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    public boolean getVanishBefore() {
        return vanishBefore;
    }

    public boolean getVanishAfter() {
        return vanishAfter;
    }

    /**
     * This forces a state, if you cancel the event, it may not be certain if the player will be visible or not.
     *
     * @param visible
     */
    public void setVanishAfter(boolean visible) {
        vanishAfter = visible;
    }

    public @Nullable UUID getUniqueId() {
        return uuid;
    }

    public @NotNull String getPlayerName() {
        return playerName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public @Nullable Player getPlayer() {
        return uuid == null ? null : Bukkit.getPlayer(uuid);
    }

    public boolean isPlayerOnline() {
        Player player = getPlayer();
        if (player == null) {
            return false;
        }
        return player.isOnline();
    }

}
