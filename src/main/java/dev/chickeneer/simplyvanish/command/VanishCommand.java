package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.PlayerVanishConfig;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@CommandAlias("vanish|van")
@CommandPermission("simplyvanish.vanish.other|simplyvanish.vanish.self")
@Description("Ensure you or another player is vanished")
public class VanishCommand extends SimplyVanishBaseCommand {
    public VanishCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }
    //(/vanish | /vanish <player>), optional: flags

    @Default
    private void vanishCommand(CommandSender sender, String[] args) {
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
            doVanish(sender, args, player.getName(), player.getUniqueId(), len, hasFlags);
        } else if (len == 1) {
            String name = args[0];
            PlayerVanishConfig config = core.getVanishConfig(name);
            if (config != null) {
                doVanish(sender, args, config.getName(), config.getUniqueId(), len, hasFlags);
                return;
            }
            // Make sure the other player is vanished...
            int finalLen = len;
            boolean finalHasFlags = hasFlags;
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(name))
                        .syncLast(player -> {
                            if (player.getName() == null) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Unknown player: " + name);
                                return;
                            }
                            doVanish(sender, args, player.getName(), player.hasPlayedBefore() ? player.getUniqueId() : null, finalLen, finalHasFlags);
                        }).execute();
        } else {
            unrecognized(sender);
        }
    }

    private void doVanish(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String name, @Nullable UUID uuid, int len, boolean hasFlags) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (!Utils.checkPerm(sender, "simplyvanish.vanish." + (other ? "other" : "self"))) {
            return;
        }
        // Make sure the player is vanished...
        if (hasFlags) {
            core.setFlags(name, uuid, args, len, sender, false, other, false);
        }
        if (core.setVanished(name, uuid, true)) {
            if (other) {
                Utils.send(sender, SimplyVanish.msgLabel + "Vanish player: " + name);
            }
        } else {
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
        }
        if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display." + (other ? "other" : "self"))) {
            core.onShowFlags(sender, name, uuid);
        }
    }
}
