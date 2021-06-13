package dev.chickeneer.simplyvanish.api.hooks.impl;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.api.hooks.AbstractHook;
import dev.chickeneer.simplyvanish.api.hooks.HookListener;
import dev.chickeneer.simplyvanish.api.hooks.HookPurpose;
import dev.chickeneer.simplyvanish.api.hooks.util.HookPluginGetter;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DynmapHook extends AbstractHook {

    private final HookPluginGetter<Plugin> getter;

    public DynmapHook() {
        getter = new HookPluginGetter<>("dynmap");
		if (getter.getPlugin() == null) {
			throw new RuntimeException("Dynmap not found."); // To let it fail.
		}
    }

    @Override
    public @NotNull String getHookName() {
        return "dynmap";
    }

    @Override
    public @NotNull HookPurpose[] getSupportedMethods() {
        return new HookPurpose[]{HookPurpose.LISTENER, HookPurpose.AFTER_REAPPEAR, HookPurpose.AFTER_VANISH};
    }

    @Override
    public @Nullable HookListener getListener() {
        return getter;
    }

    @Override
    public void afterVanish(@NotNull String name, @Nullable UUID uuid) {
        adjust(name, uuid);
    }

    @Override
    public void afterReappear(@NotNull String name, @Nullable UUID uuid) {
        adjust(name, uuid);
    }

    private void adjust(@NotNull String name, @Nullable UUID uuid) {
        boolean vanished;
        if (uuid != null) {
            vanished = SimplyVanish.isVanished(uuid);
        } else {
            vanished = SimplyVanish.isVanished(name);
        }
        DynmapAPI plg = (DynmapAPI) getter.getPlugin();
        plg.assertPlayerInvisibility(name, vanished, "SimplyVanish");
        //plg.setPlayerVisiblity(name, vanished);
    }

}
