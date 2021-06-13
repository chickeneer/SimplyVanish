package dev.chickeneer.simplyvanish.api.hooks.util;

import dev.chickeneer.simplyvanish.api.hooks.HookListener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class HookPluginGetter<T extends Plugin> extends PluginGetter<T> implements HookListener {

    public HookPluginGetter(@NotNull String pluginName) {
        super(pluginName);
    }

    @Override
    public boolean unregisterEvents() {
        // Override once it exists in bukkit.
        return false;
    }
}
