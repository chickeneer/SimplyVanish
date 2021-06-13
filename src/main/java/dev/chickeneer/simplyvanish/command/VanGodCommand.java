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

@CommandAlias("vangod")
@CommandPermission("simplyvanish.god.other|simplyvanish.god.self")
@Description("Enable god mode.")
public class VanGodCommand extends SimplyVanishBaseCommand {
    public VanGodCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }

    @Default
    private void vanGod(CommandSender sender, @Optional @Single String name) {
        // TODO: maybe later accept flags.

        if (name != null) {
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(name))
                        .syncLast(player -> {
                            if (player.getName() == null || !player.hasPlayedBefore()) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Player has not connected to this server before: " + name);
                                return;
                            }
                            doGod(sender, player.getName(), player.getUniqueId());
                        }).execute();
        } else {
            if (!Utils.checkPlayer(sender)) {
                return;
            }
            Player player = (Player) sender;
            doGod(sender, player.getName(), player.getUniqueId());
        }
    }

    private void doGod(@NotNull CommandSender sender, @NotNull String name, @NotNull UUID uuid) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (!Utils.checkPerm(sender, "simplyvanish.god." + (other ? "other" : "self"))) {
            return;
        }
        core.setGod(name, uuid, true, sender);
    }
}
