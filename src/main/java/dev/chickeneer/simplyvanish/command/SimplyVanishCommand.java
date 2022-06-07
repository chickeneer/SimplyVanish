package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.annotation.*;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.Flag;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.inventories.InventoryUtil;
import dev.chickeneer.simplyvanish.util.Formatting;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("simplyvanish|simvan")
@Description("for: reload | save | stats [reset] | flags")
public class SimplyVanishCommand extends SimplyVanishBaseCommand {
    public SimplyVanishCommand(@NotNull SimplyVanishCore core) {
        super(core);
    }

    @Subcommand("reload")
    @CommandPermission("simplyvanish.reload")
    public void onReload(CommandSender sender) {
        core.getPlugin().reloadSettings();
        Utils.sendMsg(sender, SimplyVanish.MSG_LABEL + Formatting.YELLOW + "Settings reloaded.");
    }

    @Subcommand("save")
    @CommandPermission("simplyvanish.save")
    public void onSave(CommandSender sender) {
        core.doSaveVanished();
        Utils.sendMsg(sender, SimplyVanish.MSG_LABEL + "Saved vanished configs.");
    }

    @Subcommand("stats")
    @CommandPermission("simplyvanish.stats.display")
    public void onStats(CommandSender sender, @Optional String arg) {
        Utils.sendMsg(sender, SimplyVanish.STATS.getStatsStr(true));
    }

    @Subcommand("stats reset")
    @CommandPermission("simplyvanish.stats.reset")
    public void onStatsReset(CommandSender sender) {
        SimplyVanish.STATS.clear();
        Utils.sendMsg(sender, SimplyVanish.MSG_LABEL + "Stats reset.");
    }

    @Subcommand("flags")
    @CommandPermission("simplyvanish.flags.display.self|simplyvanish.flags.display.other")
    @Default
    public void onFlags(CommandSender sender) {
        if (!core.hasPermission(sender, "simplyvanish.flags.display.self") &&
            !core.hasPermission(sender, "simplyvanish.flags.display.other")) {
            Utils.noPerm(sender);
            return;
        }
        StringBuilder b = new StringBuilder();
        for (Flag flag : new VanishConfig().getAllFlags()) {
            b.append(" ").append(Flag.fs(flag.preset)).append(flag.name);
        }
        Utils.sendMsg(sender, SimplyVanish.MSG_LABEL + "All default flags: " + Formatting.YELLOW + b);
    }

    @Subcommand("drop")
    @CommandPermission("simplyvanish.cmd.drop")
    public void onDrop(Player player) {
        Utils.dropItemInHand(player);
    }

    @CommandAlias("vanished|vand")
    @Subcommand("list")
    @CommandPermission("simplyvanish.vanished")
    @Description("Show a list of vanished players.")
    public void onList(CommandSender sender) {
        Utils.sendMsg(sender, core.getVanishedMessage());
    }

    @CommandAlias("vanpeek")
    @Subcommand("peek")
    @CommandPermission("simplyvanish.inventories.peek.at-all")
    @Description("Peek into other players inventories.")
    public void onPeek(CommandSender sender, @Flags("other") Player player) {
        InventoryUtil.showInventory(sender, (sender instanceof Player) ? core.getVanishConfig((Player) sender, true) : null, player, core.getSettings());
    }
}
