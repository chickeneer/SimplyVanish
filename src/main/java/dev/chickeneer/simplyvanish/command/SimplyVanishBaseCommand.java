package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.BaseCommand;
import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class SimplyVanishBaseCommand extends BaseCommand {
    final SimplyVanishCore core;
    Set<String> oneOfPermissions = new HashSet<>();

    public SimplyVanishBaseCommand(@NotNull SimplyVanishCore core) {
        super();
        this.core = core;
    }

    public static void unrecognized(@NotNull CommandSender sender) {
        Utils.send(sender, SimplyVanish.msgLabel + ChatColor.DARK_RED + "Unrecognized command or number of arguments.");
    }
}
