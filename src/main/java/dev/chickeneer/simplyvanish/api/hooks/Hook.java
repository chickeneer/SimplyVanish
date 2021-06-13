package dev.chickeneer.simplyvanish.api.hooks;

import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * For hooks to add for vanish.<br>
 * Players might not be online or even might not exist at all.<br>
 * boolean results will lead to SimplyVanish not performing the action/change.<br>
 * The listeners will be registered with SimplyVanish as plugin.
 */
public interface Hook {

    /**
     * Identifier.
     *
     * @return
     */
    @NotNull String getHookName();

    /**
     * @return null if all are supported.
     */
    @NotNull HookPurpose[] getSupportedMethods();

    /**
     * Get an event listener.
     *
     * @return null if not desired.
     */
    @Nullable HookListener getListener();

    /**
     * Executed before any player vanishes or logs in vanished.
     *
     * @param name
     * @param uuid
     */
    void beforeVanish(@NotNull String name, @Nullable UUID uuid);

    /**
     * Executed after a player vanishes or logs in vanished.
     *
     * @param name
     * @param uuid
     */
    void afterVanish(@NotNull String name, @Nullable UUID uuid);

    /**
     * Executed before a player reappears.
     *
     * @param name
     * @param uuid
     */
    void beforeReappear(@NotNull String name, @Nullable UUID uuid);

    /**
     * Executed after a player reappears.
     *
     * @param name
     * @param uuid
     */
    void afterReappear(@NotNull String name, @Nullable UUID uuid);

    /**
     * @param name
     * @param uuid
     * @param oldCfg     (clone)
     * @param newCfg     (clone)
     */
    void beforeSetFlags(@NotNull String name, @Nullable UUID uuid, @NotNull VanishConfig oldCfg, @NotNull VanishConfig newCfg);

    /**
     * @param name
     * @param uuid
     */
    void afterSetFlags(@NotNull String name, @Nullable UUID uuid);

    /**
     * Called on updateVanishState.<br>
     * All hooks will be called, even if a hook returns false (!), see isAllowed if it was cancelled.
     *
     * @param player
     * @param hookId    The caller of updateVanishState, 0 = SimplyVanish (or an API call not specifying a hookId).
     * @param isAllowed Will be set to false and stay false, if one hook returns false on allowUpdateVanishState.
     * @return If false is returned, an update will not be performed.
     */
    boolean allowUpdateVanishState(@NotNull Player player, int hookId, boolean isAllowed);

    /**
     * @param player
     * @param canSee
     * @param isAllowed
     * @return
     */
    boolean allowShow(@NotNull Player player, @NotNull Player canSee, boolean isAllowed);

    /**
     * @param player
     * @param canNotSee
     * @param isAllowed
     * @return
     */
    boolean allowHide(@NotNull Player player, @NotNull Player canNotSee, boolean isAllowed);

}
