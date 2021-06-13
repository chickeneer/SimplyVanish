package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.*;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@CommandAlias("vanungod")
@CommandPermission("simplyvanish.ungod.other|simplyvanish.ungod.self")
@Description("Disable god mode.")
public class VanUngodCommand extends SimplyVanishBaseCommand {
    public VanUngodCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }

    @Default
    private void vanUngod(CommandSender sender, @Optional @Single String name) {
        // TODO: maybe later accept flags.

        if (name != null) {
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(name))
                        .syncLast(player -> {
                            if (player.getName() == null || !player.hasPlayedBefore()) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Player has not connected to this server before: " + name);
                                return;
                            }
                            doUngod(sender, player.getName(), player.getUniqueId());
                        }).execute();
        } else {
            if (!Utils.checkPlayer(sender)) {
                return;
            }
            Player player = (Player) sender;
            doUngod(sender, player.getName(), player.getUniqueId());
        }
    }

    private void doUngod(@NotNull CommandSender sender, @NotNull String name, @NotNull UUID uuid) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (!Utils.checkPerm(sender, "simplyvanish.ungod." + (other ? "other" : "self"))) {
            return;
        }
        core.setGod(name, uuid, false, sender);
    }
}
