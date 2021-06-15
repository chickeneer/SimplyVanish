package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.*;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.PlayerVanishConfig;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            PlayerVanishConfig config = core.getVanishConfig(name);
            if (config != null) {
                doUngod(sender, config.getName(), config.getUniqueId());
                return;
            }
            SimplyVanish.newChain()
                        .asyncFirst(() -> Bukkit.getServer().getOfflinePlayer(name))
                        .syncLast(player -> {
                            if (player.getName() == null) {
                                Utils.send(sender, SimplyVanish.msgLabel + "Unknown player: " + name);
                                return;
                            }
                            doUngod(sender, player.getName(), player.hasPlayedBefore() ? player.getUniqueId() : null);
                        }).execute();
        } else {
            if (!Utils.checkPlayer(sender)) {
                return;
            }
            Player player = (Player) sender;
            doUngod(sender, player.getName(), player.getUniqueId());
        }
    }

    private void doUngod(@NotNull CommandSender sender, @NotNull String name, @Nullable UUID uuid) {
        boolean other = !name.equalsIgnoreCase(sender.getName());
        if (!Utils.checkPerm(sender, "simplyvanish.ungod." + (other ? "other" : "self"))) {
            return;
        }
        core.setGod(name, uuid, false, sender);
    }
}
