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

@CommandAlias("vanflag|flagvan")
@CommandPermission("simplyvanish.flags.display.other|simplyvanish.flags.display.self|simplyvanish.flags.set.other|simplyvanish.flags.set.self")
@Description("Set or display flags for yourself or others. *clear to restore default flags, to see all possible flags use: /simvan flags")
public class VanFlagCommand extends SimplyVanishBaseCommand {
    public VanFlagCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }

    @Default
    private void flagCommand(CommandSender sender, String[] args) {
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
        if (len == 0) {
            if (!Utils.checkPlayer(sender)) {
                return;
            }
            Player player = (Player) sender;
            doVanFlag(sender, args, len, hasFlags, player.getName(), player.getUniqueId());
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
                            doVanFlag(sender, args, finalLen, finalHasFlags, player.getName(), player.getUniqueId());
                        }).execute();
        } else {
            unrecognized(sender);
        }
    }

    private void doVanFlag(@NotNull CommandSender sender, @NotNull String[] args, int len, boolean hasFlags, @NotNull String name, @NotNull UUID uuid) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (hasFlags) {
            core.setFlags(name, uuid, args, len, sender, false, other, true);
        }
        if (SimplyVanish.hasPermission(sender, "simplyvanish.flags.display." + (other ? "other" : "self"))) {
            core.onShowFlags(sender, uuid);
        } else if (!hasFlags) {
            sender.sendMessage(SimplyVanish.msgLabel + ChatColor.RED + "You do not have permission to display flags" + (other ? " of others." : "."));
        }
    }
}
