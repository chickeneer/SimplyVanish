package me.asofold.bpl.simplyvanish.api.hooks.impl;

import me.asofold.bpl.simplyvanish.SimplyVanish;
import me.asofold.bpl.simplyvanish.api.hooks.AbstractHook;
import me.asofold.bpl.simplyvanish.api.hooks.HookListener;
import me.asofold.bpl.simplyvanish.api.hooks.HookPurpose;
import me.asofold.bpl.simplyvanish.api.hooks.util.HookPluginGetter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;

public class DynmapHook extends AbstractHook {

    private final HookPluginGetter<Plugin> getter;

    public DynmapHook() {
        getter = new HookPluginGetter<>("dynmap");
		if (getter.getPlugin() == null) {
			throw new RuntimeException("Dynmap not found."); // To let it fail.
		}
    }

    @Override
    public String getHookName() {
        return "dynmap";
    }

    @Override
    public HookPurpose[] getSupportedMethods() {
        return new HookPurpose[]{HookPurpose.LISTENER, HookPurpose.AFTER_REAPPEAR, HookPurpose.AFTER_VANISH};
    }

    @Override
    public HookListener getListener() {
        return getter;
    }

    @Override
    public void afterVanish(String playerName) {
        adjust(playerName);
    }

    @Override
    public void afterReappear(String playerName) {
        adjust(playerName);
    }

    private void adjust(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
		if (player != null) {
			playerName = player.getName(); // TODO...
		}
        boolean vanished = SimplyVanish.isVanished(playerName);
        DynmapAPI plg = (DynmapAPI) getter.getPlugin();
        plg.assertPlayerInvisibility(playerName, vanished, "SimplyVanish");
        //plg.setPlayerVisiblity(playerName, vanished);
    }

}
