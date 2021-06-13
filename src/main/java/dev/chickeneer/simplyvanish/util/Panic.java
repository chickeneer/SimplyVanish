package dev.chickeneer.simplyvanish.util;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class Panic {

    /**
     * Do online checking and also check settings if to continue.
     *
     * @param player1 The player to be shown or hidden.
     * @param player2
     * @param tag
     * @return true if to continue false if to abort.
     */
    public static boolean checkInvolved(@NotNull Player player1, @NotNull Player player2, @NotNull String tag, boolean noAbort) {
        boolean inconsistent = false;
        if (!Utils.checkOnline(player1, tag)) {
            inconsistent = true;
        }
        if (!Utils.checkOnline(player2, tag)) {
            inconsistent = true;
        }
        if (noAbort) {
            return true;
        } else if (inconsistent) {
            try {
                player1.sendMessage(SimplyVanish.msgLabel + ChatColor.RED + "Warning: Could not use " + tag + " to player: " + player2.getName());
            } catch (Exception t) {
            }
        }
        return !inconsistent; // "true = continue = not inconsistent"
    }

    public static void onPanic(@NotNull Settings settings, @NotNull Player[] involved) {
        Server server = Bukkit.getServer();
        if (settings.panicKickAll) {
            for (Player player : server.getOnlinePlayers()) {
                try {
                    player.kickPlayer(settings.panicKickMessage);
                } catch (Exception t) {
                    // ignore
                }
            }
        } else if (settings.panicKickInvolved) {
            for (Player player : involved) {
                try {
                    player.kickPlayer(settings.panicKickMessage);
                } catch (Exception t) {
                    // ignore
                }
            }
        }
        try {
            Utils.sendToTargets(settings.panicMessage, settings.panicMessageTargets);
        } catch (Exception t) {
            Utils.warn("[Panic] Failed to send to: " + settings.panicMessageTargets + " (" + t.getMessage() + ")");
            t.printStackTrace();
        }
        if (settings.panicRunCommand && !"".equals(settings.panicCommand)) {
            try {
                server.dispatchCommand(server.getConsoleSender(), settings.panicCommand);
            } catch (Exception t) {
                Utils.warn("[Panic] Failed to dispatch command: " + settings.panicCommand + " (" + t.getMessage() + ")");
                t.printStackTrace();
            }
        }
    }

}
