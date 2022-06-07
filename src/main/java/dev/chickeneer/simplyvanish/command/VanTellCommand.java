package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.*;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.util.Formatting;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("vantell|tellvan")
@CommandPermission("simplyvanish.cmd.vantell")
@Description("Whisper to other players, respects see state, tell flag, special permissions.")
public class VanTellCommand extends SimplyVanishBaseCommand {
    public VanTellCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }

    @Default
    private void onVantell(CommandSender sender, @Flags("other") Player player, String message) {
        // TODO: make messages configurable.

        // Availability check:
        if (sender instanceof Player) {
            if (!((Player) sender).canSee(player)) {
                VanishConfig cfg = core.getVanishConfig(player, false);
                if (cfg == null) {
                    //TODO: Need matching error message as "other" player.
                    Utils.sendMsg(sender,  Formatting.ERROR + player.getName() + " is not available.");
                    return;
                } else {
                    if (!cfg.tell.state) {
                        // check permissions (global bypass, individual bypass)
                        if (!core.hasPermission(sender, "simplyvanish.vantell.bypass") &&
                            !core.hasPermission(sender, "simplyvanish.vantell.bypass.player." + player.getName().toLowerCase())) {
                            Utils.sendMsg(sender, Formatting.ERROR + player.getName() + " is not available.");
                            return;
                        }
                    }
                }
            }
        }

        // Message:
        Utils.sendMsg(player, Formatting.GRAY + sender.getName() + " whispers:" + message);
        // Log if desired
        // TODO: check settings for log and probably log.
        Settings settings = core.getSettings();
        if (settings.logVantell) {
            Utils.info("[vantell] (" + sender.getName() + " -> " + player.getName() + ")" + message);
        }
        if (settings.mirrorVantell) {
            Utils.sendMsg(sender, Formatting.DARK_GRAY + "(-> " + player.getName() + ")" + message);
        }
    }

}
