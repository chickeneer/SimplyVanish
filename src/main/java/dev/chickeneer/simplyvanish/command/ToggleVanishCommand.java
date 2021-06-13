package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@CommandAlias("tvanish|vanisht|tvan|vant")
@CommandPermission("simplyvanish.reappear.other|simplyvanish.reappear.self|simplyvanish.vanish.other|simplyvanish.vanish.self")
@Description("Toggle your or another players visibility state")
public class ToggleVanishCommand extends SimplyVanishBaseCommand {

    public ToggleVanishCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }
    //(/tvanish | /tvanish <player>), optional: flags.

    @Default
    private void toggleVanishCommand(CommandSender sender, String[] args) {
        int len = args.length;
        boolean hasFlags = false;
        // reduce len by number of flags.
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i].startsWith("+") || args[i].startsWith("-") || args[i].startsWith("*")) {
                len--;
                hasFlags = true;
            } else {
                break;
            }
        }
        boolean other;
        if (len == 0) {
            if (!Utils.checkPlayer(sender)) {
                return;
            }
            Player player = (Player) sender;
            doToggle(sender, args, player.getName(), player.getUniqueId(), len, hasFlags);
        } else if (len == 1) {
            int finalLen = len;
            boolean finalHasFlags = hasFlags;
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(args[0]))
                        .syncLast(player -> {
                            if (player.getName() == null || !player.hasPlayedBefore()) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Player has not connected to this server before: " + args[0]);
                                return;
                            }
                            doToggle(sender, args, player.getName(), player.getUniqueId(), finalLen, finalHasFlags);
                        }).execute();
        } else {
            unrecognized(sender);
        }
    }

    private void doToggle(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String name, @NotNull UUID uuid, int len, boolean hasFlags) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        boolean doVanish = !core.isVanished(name, uuid);
        if (!Utils.checkPerm(sender, "simplyvanish." + (doVanish ? "vanish" : "reappear") + "." + (other ? "other" : "self"))) {
            return;
        }
        if (hasFlags) {
            core.setFlags(name, uuid, args, len, sender, false, other, false);
        }
        if (core.setVanished(name, uuid, doVanish)) {
            if (other) {
                if (doVanish) {
                    Utils.send(sender, SimplyVanish.msgLabel + "Vanish player: " + name);
                } else {
                    Utils.send(sender, SimplyVanish.msgLabel + "Show player: " + name);
                }
            }
        } else {
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
        }
        String displayPerm = "simplyvanish.display." + (other ? "other" : "self");
        if (hasFlags && SimplyVanish.hasPermission(sender, displayPerm)) {
            core.onShowFlags(sender, uuid);
        }
    }
}
