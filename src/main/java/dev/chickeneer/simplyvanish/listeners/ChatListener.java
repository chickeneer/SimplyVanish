package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.util.Formatting;
import dev.chickeneer.simplyvanish.util.Utils;
import io.papermc.paper.event.player.AsyncChatEvent;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Chat + commands
 */
public final class ChatListener implements Listener {
    private final SimplyVanishCore core;

    public ChatListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    @SuppressWarnings("unused")
    private boolean shouldCancelChat(@NotNull Player player) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        return shouldCancelChat(cfg);
    }

    @Contract("null -> false")
    private static boolean shouldCancelChat(@Nullable VanishConfig cfg) {
        if (cfg == null) {
            return false;
        }
        return cfg.vanished.state && !cfg.chat.state;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onChat(AsyncChatEvent event) {
        // Just prevent accidental chat.
        Player player = event.getPlayer();
        VanishConfig vConfig = core.getVanishConfig(player, false);
        if (shouldCancelChat(vConfig)) {
            event.setCancelled(true);
            if (vConfig.notify.state) {
                Utils.sendMsg(player, SimplyVanish.MSG_LABEL + Formatting.ERROR + "Disabled! (/vanflag +chat or /reappear)");
            }
        }
    }

    /**
     * @param cfg Player config
     * @param cmd  command (lower case, trim).
     * @return
     */
    @Contract("null, _ -> false")
    private boolean shouldCancelCmd(@Nullable VanishConfig cfg, @NotNull String cmd) {
        if (cfg == null) {
            return false;
        }
        if (!cfg.vanished.state) {
            return false;
        }
        if (!cfg.cmd.state) {
            Settings settings = core.getSettings();
            boolean contains = settings.cmdCommands.contains(cmd);
            if (settings.cmdWhitelist) {
                return !contains;
            } else {
                return contains;
            }
        } else {
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().trim().split(" ", 2)[0].toLowerCase();
        if (cmd.length() > 1) {
            cmd = cmd.substring(1);
        }
        // TODO: maybe find the fastest way to do this !
        Player player = event.getPlayer();
        VanishConfig vConfig = core.getVanishConfig(player, false);
        if (shouldCancelCmd(vConfig, cmd)) {
            event.setCancelled(true);
            if (vConfig.notify.state) {
                Utils.sendMsg(player, SimplyVanish.MSG_LABEL + Formatting.ERROR + "Disabled! (/vanflag +cmd or /reappear)");
            }
        }
    }
}
