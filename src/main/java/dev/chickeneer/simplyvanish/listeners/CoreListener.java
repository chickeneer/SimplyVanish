package dev.chickeneer.simplyvanish.listeners;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.api.events.SimplyVanishAtLoginEvent;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.util.HookUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public final class CoreListener implements Listener {
    private final SimplyVanishCore core;

    public CoreListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerQuit(PlayerQuitEvent event) {
        if (onLeave(event.getPlayer(), " quit.")) {
            event.quitMessage(null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerKick(PlayerKickEvent event) {
        if (onLeave(event.getPlayer(), " was kicked.")) {
            event.leaveMessage(Component.empty());
        }
    }

    /**
     * For Quit / kick.
     *
     * @param player
     * @param action
     * @return If to clear the leave message.
     */
    boolean onLeave(@NotNull Player player, @NotNull String action) {
        Settings settings = core.getSettings();
        if (settings.suppressQuitMessage && core.isVanished(player)) {
            boolean online = core.getVanishConfig(player, true).online.state;
            // TODO: Not sure about the notify flag here.
            if (settings.notifyState && !online) {
                String msg = SimplyVanish.msgLabel + ChatColor.GREEN + player.getName() + ChatColor.GRAY + action;
                for (Player other : Bukkit.getServer().getOnlinePlayers()) {
                    if (core.hasPermission(other, settings.notifyStatePerm)) {
                        other.sendMessage(msg);
                    }
                }
            }
            return !online; // suppress in any case if vanished.
        } else {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onServerListPing(PaperServerListPingEvent event) {
        Iterator<Player> it = event.iterator();

        while (it.hasNext()) {
            VanishConfig cfg = core.getVanishConfig(it.next(), false);
            if (cfg != null && cfg.vanished.state && !cfg.online.state) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Settings settings = core.getSettings();
        VanishConfig cfg = core.getVanishConfig(player, false);
        boolean wasVanished = cfg != null && cfg.vanished.state;
        boolean auto = false; // Indicate if the player should be vanished due to auto-vanish.
        if (settings.autoVanishUse && (cfg == null || cfg.auto.state)) {
            if (core.hasPermission(player, settings.autoVanishPerm)) {
                // permission given, do attempt to vanish
                auto = true;
                if (cfg == null) {
                    cfg = core.getVanishConfig(player, true);
                }
            }
        }
        boolean doVanish = auto || wasVanished;
        HookUtil hookUtil = core.getHookUtil();
        if (doVanish) {
            SimplyVanishAtLoginEvent svEvent = new SimplyVanishAtLoginEvent(player, wasVanished, true, auto);
            Bukkit.getServer().getPluginManager().callEvent(svEvent);
            if (svEvent.isCancelled()) {
                // no update
                return;
            }
            doVanish = svEvent.getVanishAfter();
            cfg.set("vanished", doVanish);
            if (doVanish) {
                hookUtil.callBeforeVanish(player.getName(), player.getUniqueId()); // need to check again.
            }
        }
        if (!core.updateVanishState(player)) {
            // TODO: set doVanish ? remove from vanished ?
            return;
        }
        if (doVanish) {
            hookUtil.callAfterVanish(player.getName(), player.getUniqueId());
            if (settings.suppressJoinMessage && cfg.vanished.state) {
                if (!cfg.online.state) {
                    event.joinMessage(null);
                }
            } else if (!cfg.needsSave()) {
                core.removeVanished(player.getName(), player.getUniqueId());
            }
        }
    }
}
