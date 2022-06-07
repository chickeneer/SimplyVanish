package dev.chickeneer.simplyvanish.util;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.config.compatlayer.CompatConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * Some static methods for more generic purpose.
 */
public class Utils {

    /**
     * Check, message on failure.
     *
     * @param sender
     * @param perm
     * @return
     */
    public static boolean checkPerm(@NotNull CommandSender sender, @NotNull String perm) {
        if (!SimplyVanish.getInstance().hasPermission(sender, perm)) {
            Utils.noPerm(sender);
            return false;
        }
        return true;
    }

    /**
     * Intended as direct return value for onCommand.
     *
     * @param sender
     */
    public static void noPerm(@NotNull CommandSender sender) {
        Utils.sendMsg(sender, Formatting.SEVERE + "[SimplyVanish] No permission.");
    }

    /**
     * Check if is a player, message if not.
     *
     * @param sender
     * @return
     */
    public static boolean checkPlayer(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        Utils.sendMsg(sender, "[SimplyVanish] This is only available for players!");
        return false;
    }

    public static boolean forceDefaults(@NotNull Configuration defaults, @NotNull CompatConfig config) {
        Map<String, Object> all = defaults.getValues(true);
        boolean changed = false;
        for (String path : all.keySet()) {
            if (!config.hasEntry(path)) {
                config.setProperty(path, defaults.get(path));
                changed = true;
            }
        }
        return changed;
    }

    public static boolean checkOnline(@NotNull Player player) {
        return checkOnline(player.getName());
    }

    public static boolean checkOnline(@NotNull String name) {
        final Player player = Bukkit.getServer().getPlayerExact(name);
        return player != null;
    }

    /**
     * Check and log warning message.
     *
     * @param player
     * @param tag
     * @return
     */
    public static boolean checkOnline(@NotNull Player player, @NotNull String tag) {
        final boolean res = checkOnline(player);
        if (!res) {
            warn("[" + tag + "] Inconsistent online state (flag=" + player.isOnline() + ") Server returns null for: " + player.getName());
        }
        return res;
    }

    public static void info(@NotNull String msg) {
        Bukkit.getServer().getLogger().info("[SimplyVanish] " + msg);
    }

    public static void warn(@NotNull String msg) {
        Bukkit.getServer().getLogger().warning("[SimplyVanish] " + msg);
    }

    public static void severe(@NotNull String msg) {
        Bukkit.getServer().getLogger().severe("[SimplyVanish] " + msg);
    }

    public static void severe(@NotNull String msg, @NotNull Throwable t) {
        severe(msg);
        severe(t);
    }

    public static void severe(@NotNull Throwable t) {
        severe(toString(t));
    }

    public static void warn(@NotNull String msg, @NotNull Throwable t) {
        warn(msg);
        warn(t);
    }

    public static void warn(@NotNull Throwable t) {
        warn(toString(t));
    }

    public static @NotNull String toString(@NotNull Throwable t) {
        final Writer buf = new StringWriter(500);
        final PrintWriter writer = new PrintWriter(buf);
        t.printStackTrace(writer);
        // TODO: maybe make lines and log one by one.
        return buf.toString();
    }

    public static void sendToTargets(@NotNull String msg, @NotNull String targetSpec) {
        // check targets:
        List<Player> players = new LinkedList<>();
        Collection<? extends Player> online = Bukkit.getServer().getOnlinePlayers();
        for (String x : targetSpec.split(",")) {
            String targets = x.trim();
            if (targets.equalsIgnoreCase("ops") || targets.equalsIgnoreCase("operators")) {
                for (Player player : online) {
                    if (player.isOp()) {
                        players.add(player);
                    }
                }
            } else if (targets.equalsIgnoreCase("all") || targets.equalsIgnoreCase("everyone") || (targets.equalsIgnoreCase("everybody"))) {
                players.addAll(online);
            } else if (targets.toLowerCase().startsWith("permission:") && targets.length() > 11) {
                String perm = targets.substring(11).trim();
                for (Player player : online) {
                    if (SimplyVanish.getInstance().hasPermission(player, perm)) {
                        players.add(player);
                    }
                }
            }
        }
        for (Player player : players) {
            Utils.sendMsg(player, msg);
        }
    }

    public static void dropItemInHand(@NotNull Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType() == Material.AIR) {
            return;
        }
        ItemStack newStack = stack.clone();
        Item item = player.getWorld().dropItem(player.getLocation().add(new Vector(0.0, 1.0, 0.0)), newStack);
        if (!item.isDead()) {
            item.setVelocity(player.getLocation().getDirection().normalize().multiply(0.05));
            player.getInventory().setItemInMainHand(null);
        }
    }

    public static void sendMsg(@NotNull CommandSender sender, @NotNull String miniMessage, @NotNull TagResolver @NotNull ... tagResolver) {
        Utils.sendMsg(sender, deserializeMessage(miniMessage, tagResolver));
    }

    public static void sendMsg(@NotNull CommandSender sender, @NotNull Component message) {
        sender.sendMessage(message);
    }

    public static Component deserializeMessage(@NotNull String message, @NotNull TagResolver @NotNull ... tagResolver) {
        return MiniMessage.miniMessage().deserialize(message, tagResolver);
    }

    /**
     * @param parts
     * @param link  can be null
     * @return
     */
    public static @NotNull String join(@NotNull Collection<String> parts, @Nullable String link) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        int max = parts.size();
        for (String part : parts) {
            builder.append(part);
            i++;
            if (i < max && link != null) {
                builder.append(link);
            }
        }
        return builder.toString();
    }

    public static void sendBlock(@NotNull Player player, @NotNull Block block) {
        block.getState().update();
        // player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
    }

    public static void tryMessage(@Nullable UUID uuid, @NotNull String msg) {
        if (uuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        Utils.sendMsg(player, msg);
    }

    /**
     * Get an entity if the shooter is an entity, otherwise return null.
     *
     * @param projectile
     * @return
     */
    public static @Nullable Entity getShooterEntity(@NotNull Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        if (source instanceof Entity) {
            return (Entity) source;
        }
        return null;
    }
}
