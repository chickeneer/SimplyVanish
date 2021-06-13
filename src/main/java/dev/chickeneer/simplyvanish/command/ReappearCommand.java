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

@CommandAlias("reappear|rea")
@CommandPermission("simplyvanish.reappear.other|simplyvanish.reappear.self")
@Description("Ensure you or another player is visible")
public class ReappearCommand extends SimplyVanishBaseCommand {
    public ReappearCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }
    //(/reappear | /vanish <player>), optional: flags

    @Default
    private void reappearCommand(CommandSender sender, String[] args) {
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
            doReappear(sender, args, player.getName(), player.getUniqueId(), len, hasFlags);
        } else if (len == 1) {
            boolean finalHasFlags = hasFlags;
            int finalLen = len;
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(args[0]))
                        .syncLast(player -> {
                            if (player.getName() == null || !player.hasPlayedBefore()) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Player has not connected to this server before: " + args[0]);
                                return;
                            }
                            doReappear(sender, args, player.getName(), player.getUniqueId(), finalLen, finalHasFlags);
                        }).execute();
        } else {
            unrecognized(sender);
        }
    }

    private void doReappear(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String name, @NotNull UUID uuid, int len, boolean hasFlags) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (!SimplyVanish.hasPermission(sender, "simplyvanish.reappear." + (other ? "other" : "self"))) {
            Utils.noPerm(sender);
            return;
        }
        if (hasFlags) {
            core.setFlags(name, uuid, args, len, sender, false, other, false);
        }
        if (core.setVanished(name, uuid, false)) {
            if (other) {
                Utils.send(sender, SimplyVanish.msgLabel + "Show player: " + name);
            }
        } else {
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
        }
        if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display." + (other ? "other" : "self"))) {
            core.onShowFlags(sender, uuid);
        }
    }
}
