package dev.chickeneer.simplyvanish.command;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.PaperCommandManager;
import dev.chickeneer.simplyvanish.SimplyVanishCore;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class SimplyVanishCommandManager extends PaperCommandManager {
    final SimplyVanishCore core;

    public SimplyVanishCommandManager(@NotNull Plugin plugin, @NotNull SimplyVanishCore core) {
        super(plugin);
        this.core = core;
    }

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern PIPE = Pattern.compile("\\|");

    @Override
    public boolean hasPermission(CommandIssuer issuer, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        String[] perms = COMMA.split(permission);
        if (perms.length > 1) {
            return hasPermission(issuer, new HashSet<>(Arrays.asList(perms)));
        } else {
            CommandSender sender = issuer.getIssuer();
            for (String perm : PIPE.split(perms[0])) {
                perm = perm.trim();
                if (!perm.isEmpty() && core.hasPermission(sender, perm)) {
                    return true;
                }
            }
            return false;
        }
    }
}
