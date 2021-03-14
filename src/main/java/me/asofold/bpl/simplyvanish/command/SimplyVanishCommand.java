package me.asofold.bpl.simplyvanish.command;

import me.asofold.bpl.simplyvanish.SimplyVanish;
import me.asofold.bpl.simplyvanish.SimplyVanishCore;
import me.asofold.bpl.simplyvanish.config.Flag;
import me.asofold.bpl.simplyvanish.config.Path;
import me.asofold.bpl.simplyvanish.config.Settings;
import me.asofold.bpl.simplyvanish.config.VanishConfig;
import me.asofold.bpl.simplyvanish.config.compatlayer.CompatConfig;
import me.asofold.bpl.simplyvanish.inventories.InventoryUtil;
import me.asofold.bpl.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.*;


public class SimplyVanishCommand {

    private SimplyVanishCore core;

    /**
     * Dynamic "fake" commands.
     */
    public LightCommands aliasManager = new LightCommands();

    /**
     * Map aliases to recognized labels.
     */
    public Map<String, String> commandAliases = new HashMap<String, String>();

    /**
     * All command labels (not aliases).
     */
    public static final String[] baseLabels = new String[]{
            "vanish", "reappear", "tvanish", "simplyvanish", "vanished", "vanflag", "vantell", "vanpeek"
    };

    /**
     * Command labels (not aliases) that take flags.
     */
    private final Set<String> flagLabels = new HashSet<String>(Arrays.asList(new String[]{
            "vanish", "reappear", "tvanish", "simplyvanish", "vanflag", "vangod", "vanungod"
    }));


    public SimplyVanishCommand(SimplyVanishCore core) {
        this.core = core;
    }

    /**
     * Get standardized lower-case label, possibly mapped from an alias.
     *
     * @param label
     * @return
     */
    String getMappedCommandLabel(String label) {
        label = label.toLowerCase();
        String mapped = commandAliases.get(label);
        if (mapped == null) return label;
        else return mapped;
    }

    public void registerCommandAliases(CompatConfig config, Path path) {
        SimplyVanish plugin = core.getPlugin();
        aliasManager.cmdNoOp = SimplyVanish.cmdNoOp; //  hack :)
        // Register aliases from configuration ("fake").
        aliasManager.clear();
        for (String cmd : SimplyVanishCommand.baseLabels) {
            // TODO: only register the needed aliases.
            cmd = cmd.trim().toLowerCase();
            List<String> mapped = config.getStringList("commands" + path.sep + cmd + path.sep + "aliases", null);
            if (mapped == null || mapped.isEmpty()) continue;
            List<String> needed = new LinkedList<String>(); // those that need to be registered.
            for (String alias : mapped) {
                Command ref = plugin.getCommand(alias);
                if (ref == null) {
                    needed.add(alias);
                } else if (ref.getLabel().equalsIgnoreCase(cmd)) {
                    // already mapped to that command.
                    continue;
                } else needed.add(alias);
            }
            if (needed.isEmpty()) continue;
            // register with wrong(!) label:
            if (!aliasManager.registerCommand(cmd, needed, plugin)) {
                // TODO: log maybe
            }
            Command ref = plugin.getCommand(cmd);
            if (ref != null) {
                aliasManager.removeAlias(cmd); // the command is registered already.
                for (String alias : ref.getAliases()) {
                    aliasManager.removeAlias(alias); // TODO ?
                }
            }
            for (String alias : needed) {
                alias = alias.trim().toLowerCase();
                commandAliases.put(alias, cmd);
            }

        }

        // Register aliases for commands from plugin.yml:
        for (String cmd : SimplyVanishCommand.baseLabels) {
            cmd = cmd.trim().toLowerCase();
            PluginCommand command = plugin.getCommand(cmd);
            if (command == null) continue;
            List<String> aliases = command.getAliases();
            if (aliases == null) continue;
            for (String alias : aliases) {
                commandAliases.put(alias.trim().toLowerCase(), cmd);
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
//		SimplyVanish plugin = core.getPlugin();
        label = getMappedCommandLabel(label);
        int len = args.length;
        boolean hasFlags = false;
        if (flagLabels.contains(label)) {
            // reduce len by number of flags.
            for (int i = args.length - 1; i >= 0; i--) {
                if (args[i].startsWith("+") || args[i].startsWith("-") || args[i].startsWith("*")) {
                    len--;
                    hasFlags = true;
                } else break;
            }
        }
        if (label.equals("vantell")) {
            onVantell(sender, args);
            return true;
        } else if (label.equals("vanish")) return vanishCommand(sender, args, len, hasFlags);
        else if (label.equals("reappear")) return reappearCommand(sender, args, len, hasFlags);
        else if (label.equals("tvanish")) {
            String name;
            if (len == 0) {
                if (!Utils.checkPlayer(sender)) return true;
                name = ((Player) sender).getName();
            } else if (len == 1) {
                name = args[0].trim();
                if (name.isEmpty()) return unrecognized(sender);
            } else return unrecognized(sender);
            if (!core.isVanished(name)) return vanishCommand(sender, args, len, hasFlags);
            else return reappearCommand(sender, args, len, hasFlags);
        } else if (label.equals("vanished")) {
            if (!Utils.checkPerm(sender, "simplyvanish.vanished")) return true;
            Utils.send(sender, core.getVanishedMessage());
            return true;
        } else if (label.equals("simplyvanish") || label.equals("vanflag")) {
            if (!hasFlags && label.equals("simplyvanish")) {
                if (rootCommand(sender, args)) return true;
            }
            return flagCommand(sender, args, len, hasFlags);
        } else if (label.equals("vangod")) {
            return vanGodCommand(sender, args, len, hasFlags, false);
        } else if (label.equals("vanungod")) {
            return vanGodCommand(sender, args, len, hasFlags, true);
        } else if (label.equals("vanpeek") && len == 1) {
            return vanPeekCommand(sender, args[0]);
        }
        return unrecognized(sender);
    }

    private boolean vanPeekCommand(CommandSender sender, String name) {
        name = name.trim().toLowerCase();
        if (name.isEmpty()) return false;
        if (name.equalsIgnoreCase(sender.getName())) {
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.YELLOW + "You can not peek into your own inventory :) !");
            return true;
        }
        if (!Utils.checkPerm(sender, "simplyvanish.inventories.peek.at-all")) return true; // TODO
        InventoryUtil.showInventory(sender, (sender instanceof Player) ? core.getVanishConfig(sender.getName(), true) : null, name, core.getSettings());
        return true;
    }

    private boolean vanGodCommand(CommandSender sender, String[] args,
                                  int len, boolean hasFlags, boolean ungod) {
        // TODO: maybe later accept flags.
        String perm = "simplyvanish." + (ungod ? "ungod." : "god.");
        if (len == 0) {
            if (!Utils.checkPlayer(sender)) return true;
            checkVangod(sender, perm + ".self", sender.getName(), ungod);
            return true;
        } else if (len == 1) {
            checkVangod(sender, perm + ".other", args[0].trim(), ungod);
            return true;
        }
        return unrecognized(sender);
    }

    private void checkVangod(CommandSender sender, String perm, String name,
                             boolean ungod) {
        if (!Utils.checkPerm(sender, perm)) return;
        core.setGod(name, !ungod, sender);
    }

    private boolean flagCommand(CommandSender sender, String[] args, int len,
                                boolean hasFlags) {
        if (hasFlags && len == 0) {
            if (!Utils.checkPlayer(sender)) return true;
            core.setFlags(((Player) sender).getName(), args, len, sender, false, false, true);
            if (SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.self"))
                core.onShowFlags((Player) sender, null);
            return true;
        } else if (len == 0) {
            if (!Utils.checkPlayer(sender)) return true;
            if (SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.self"))
                core.onShowFlags((Player) sender, null);
            else
                sender.sendMessage(SimplyVanish.msgLabel + ChatColor.RED + "You do not have permission to display flags.");
            return true;
        } else if (hasFlags && len == 1) {
            core.setFlags(args[0], args, len, sender, false, true, true);
            if (SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.other"))
                core.onShowFlags(sender, args[0]);
            return true;
        } else if (len == 1) {
            if (SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.other"))
                core.onShowFlags(sender, args[0]);
            else
                sender.sendMessage(SimplyVanish.msgLabel + ChatColor.RED + "You do not have permission to display flags of others.");
            return true;
        }
        return unrecognized(sender);
    }

    private boolean reappearCommand(CommandSender sender, String[] args, int len,
                                    boolean hasFlags) {
        if (len == 0) {
            if (!Utils.checkPlayer(sender)) return true;
            if (!SimplyVanish.hasPermission(sender, "simplyvanish.reappear.self")) return Utils.noPerm(sender);
            // Let the player be seen...
            if (hasFlags) core.setFlags(((Player) sender).getName(), args, len, sender, false, false, false);
            if (!SimplyVanish.setVanished((Player) sender, false))
                Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
            if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.self"))
                core.onShowFlags((Player) sender, null);
            return true;
        } else if (len == 1) {
            if (!SimplyVanish.hasPermission(sender, "simplyvanish.reappear.other")) return Utils.noPerm(sender);
            // Make sure the other player is shown...
            String name = args[0].trim();
            if (hasFlags) core.setFlags(name, args, len, sender, false, true, false);
            if (SimplyVanish.setVanished(name, false))
                Utils.send(sender, SimplyVanish.msgLabel + "Show player: " + name);
            else Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
            if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.other"))
                core.onShowFlags((Player) sender, name);
            return true;
        }
        return unrecognized(sender);
    }

    private boolean vanishCommand(CommandSender sender, String[] args, int len,
                                  boolean hasFlags) {
        if (len == 0) {
            if (!Utils.checkPlayer(sender)) return true;
            if (!Utils.checkPerm(sender, "simplyvanish.vanish.self")) return true;
            // Make sure the player is vanished...
            if (hasFlags) core.setFlags(((Player) sender).getName(), args, len, sender, false, false, false);
            if (!SimplyVanish.setVanished((Player) sender, true))
                Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
            if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.self"))
                core.onShowFlags((Player) sender, null);
            return true;
        } else if (len == 1) {
            if (!Utils.checkPerm(sender, "simplyvanish.vanish.other")) return true;
            // Make sure the other player is vanished...
            String name = args[0].trim();
            if (hasFlags) core.setFlags(name, args, len, sender, false, true, false);
            if (SimplyVanish.setVanished(name, true))
                Utils.send(sender, SimplyVanish.msgLabel + "Vanish player: " + name);
            else Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Action was prevented by hooks.");
            if (hasFlags && SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.other"))
                core.onShowFlags((Player) sender, name);
            return true;
        }
        return unrecognized(sender);
    }

    /**
     * Try to use as root command.
     *
     * @param sender
     * @param args
     * @return IF COMMAND EXECUTED
     */
    private boolean rootCommand(CommandSender sender, String[] args) {
        SimplyVanish plugin = core.getPlugin();
        int len = args.length;
        if (len == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!Utils.checkPerm(sender, "simplyvanish.reload")) return true;
            plugin.loadSettings();
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.YELLOW + "Settings reloaded.");
            return true;
        }
//		else if (len==1 && args[0].equalsIgnoreCase("drop")){
//			if ( !Utils.checkPerm(sender, "simplyvanish.cmd.drop")) return true;
//			if (!Utils.checkPlayer(sender)) return true;
//			Utils.dropItemInHand((Player) sender);
//			return true;
//		}
        else if (len == 1 && args[0].equalsIgnoreCase("save")) {
            if (!Utils.checkPerm(sender, "simplyvanish.save")) return true;
            core.doSaveVanished();
            sender.sendMessage(SimplyVanish.msgLabel + "Saved vanished configs.");
            return true;
        } else if (len == 1 && args[0].equals(SimplyVanish.cmdNoOpArg)) return true;
        else if (len == 1 && args[0].equalsIgnoreCase("stats")) {
            if (!Utils.checkPerm(sender, "simplyvanish.stats.display")) return true;
            Utils.send(sender, SimplyVanish.stats.getStatsStr(true));
            return true;
        } else if (len == 2 && args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("reset")) {
            if (!Utils.checkPerm(sender, "simplyvanish.stats.reset")) return true;
            SimplyVanish.stats.clear();
            Utils.send(sender, SimplyVanish.msgLabel + "Stats reset.");
            return true;
        } else if (len == 1 && args[0].equalsIgnoreCase("flags")) {
            if (!SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.self") && !SimplyVanish.hasPermission(sender, "simplyvanish.flags.display.other"))
                return Utils.noPerm(sender);
            VanishConfig cfg = new VanishConfig();
            StringBuilder b = new StringBuilder();
            for (Flag flag : cfg.getAllFlags()) {
                b.append(" " + Flag.fs(flag.preset) + flag.name);
            }
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.GRAY + "All default flags: " + ChatColor.YELLOW + b.toString());
            return true;
        }
        return false; // command not executed, maybe unknown.

    }

    /**
     * "vantell" command (Attempt to make it somewhat compatible with tell).
     *
     * @param sender
     * @param args
     */
    private void onVantell(CommandSender sender, String[] args) {
        // TODO: make messages configurable.
        // permissions
        if (!SimplyVanish.hasPermission(sender, "simplyvanish.cmd.vantell")) {
            sender.sendMessage(ChatColor.DARK_RED + "You don't have permission.");
            return;
        }
        // Usage check:
        if (args.length < 2) {
            sender.sendMessage(ChatColor.DARK_RED + "Whisper: You must give a player and a message !");
            return;
        }
        // Availability check:
        String playerName = args[0];
        Player other = Bukkit.getServer().getPlayerExact(playerName);
        String otherName = null;
        if (other != null) {
            otherName = other.getName();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.equals(other)) other = null;
                else if (!player.canSee(other)) {
                    VanishConfig cfg = core.getVanishConfig(otherName, false);
                    if (cfg == null) other = null; // don't let pass
                    else {
                        if (!cfg.tell.state) {
                            // check permissions (global bypass, individual bypass)
                            if (!core.hasPermission(sender, "simplyvanish.vantell.bypass") && !core.hasPermission(sender, "simplyvanish.vantell.bypass.player." + otherName.toLowerCase()))
                                other = null;
                        }
                        // else: let pass
                    }
                }
                // else: let pass
            }
            // else: let pass
        }
        if (other == null) {
            sender.sendMessage(ChatColor.RED + playerName + " is not available.");
            return;
        }

        // Message:
        StringBuilder b = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            b.append(" ");
            b.append(args[i]);
        }
        String coreMessage = b.toString();
        other.sendMessage(ChatColor.GRAY + sender.getName() + " whispers:" + coreMessage);
        // Log if desired
        // TODO: check settings for log and probably log.
        Settings settings = core.getSettings();
        if (settings.logVantell)
            Bukkit.getServer().getLogger().info("[vantell] (" + sender.getName() + " -> " + otherName + ")" + coreMessage);
        if (settings.mirrorVantell) Utils.send(sender, ChatColor.DARK_GRAY + "(-> " + otherName + ")" + coreMessage);
    }

    /**
     * Message and return false.
     *
     * @param sender
     * @return
     */
    public static boolean unrecognized(CommandSender sender) {
        Utils.send(sender, SimplyVanish.msgLabel + ChatColor.DARK_RED + "Unrecognized command or number of arguments.");
        return false;
    }

}
