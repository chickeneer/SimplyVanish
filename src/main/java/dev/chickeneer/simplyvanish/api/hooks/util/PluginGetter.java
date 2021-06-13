package dev.chickeneer.simplyvanish.api.hooks.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple plugin fetching.
 *
 * @param <T>
 */
public class PluginGetter<T extends Plugin> implements Listener {
    private T plugin = null;
    private final String pluginName;

    public PluginGetter(@NotNull String pluginName) {
        this.pluginName = pluginName;
        fetchPlugin();
    }

    /**
     * Fetch from Bukkit and set , might set to null, though.
     */
    @SuppressWarnings("unchecked")
    public final void fetchPlugin() {
        final Plugin ref = Bukkit.getPluginManager().getPlugin(pluginName);
        plugin = (T) ref;
    }

    @SuppressWarnings("unchecked")
    final void onPluginEnable(@NotNull PluginEnableEvent event) {
        final Plugin ref = event.getPlugin();
        if (plugin.getName().equals(pluginName)) {
            plugin = (T) ref;
        }
    }

    /**
     * For convenience with chaining: getX = new PluginGetter<X>("X").registerEvents(this);
     *
     * @param other
     * @return
     */
    public final @NotNull PluginGetter<T> registerEvents(@NotNull Plugin other) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return this;
    }

    public final @Nullable T getPlugin() {
        return plugin;
    }
}
