package dev.chickeneer.simplyvanish.api.hooks;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Might be useful if you only want to override few methods, does nothing by default.
 */
public abstract class AbstractHook implements Hook {

    protected final int hookId = SimplyVanish.getNewHookId();

    @Override
    public abstract @NotNull String getHookName();

    @Override
    public abstract @NotNull HookPurpose[] getSupportedMethods();

    @Override
    public @Nullable HookListener getListener() {
        return null;
    }

    @Override
    public void beforeVanish(@NotNull String name, @Nullable UUID uuid) {
    }

    @Override
    public void afterVanish(@NotNull String name, @Nullable UUID uuid) {
    }

    @Override
    public void beforeReappear(@NotNull String name, @Nullable UUID uuid) {
    }

    @Override
    public void afterReappear(@NotNull String name, @Nullable UUID uuid) {
    }

    @Override
    public void beforeSetFlags(@NotNull String name, @Nullable UUID uuid, @NotNull VanishConfig oldCfg, @NotNull VanishConfig newCfg) {
    }

    @Override
    public void afterSetFlags(@NotNull String name, @Nullable UUID uuid) {
    }

    @Override
    public boolean allowUpdateVanishState(@NotNull Player player, int hookId, boolean isAllowed) {
        return true;
    }

    @Override
    public boolean allowShow(@NotNull Player player, @NotNull Player canSee, boolean isAllowed) {
        return true;
    }

    @Override
    public boolean allowHide(@NotNull Player player, @NotNull Player canNotSee, boolean isAllowed) {
        return true;
    }

}
