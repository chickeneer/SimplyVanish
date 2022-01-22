package dev.chickeneer.simplyvanish;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import dev.chickeneer.simplyvanish.api.hooks.Hook;
import dev.chickeneer.simplyvanish.command.*;
import dev.chickeneer.simplyvanish.config.Path;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.config.compatlayer.CompatConfig;
import dev.chickeneer.simplyvanish.config.compatlayer.CompatConfigFactory;
import dev.chickeneer.simplyvanish.listeners.*;
import dev.chickeneer.simplyvanish.stats.Stats;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vanish + God mode + No Target + No pickup.
 */
public class SimplyVanish extends JavaPlugin {
    private static SimplyVanish INSTANCE;

    SimplyVanishCore core;
    SimplyVanishCommandManager commandManager;

    public static final String msgLabel = ChatColor.GOLD + "[SimplyVanish]" + ChatColor.GRAY + " ";
    public static final String msgStillInvisible =
            SimplyVanish.msgLabel + ChatColor.GRAY + "You are still " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " to normal players.";
    public static final String msgNowInvisible =
            SimplyVanish.msgLabel + ChatColor.GRAY + "You are now " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + " to normal players.";
    public static final String msgNotifyPing =
            SimplyVanish.msgLabel + ChatColor.GRAY + "You are " + ChatColor.GREEN + "invisible" + ChatColor.GRAY + ", right now.";
    public static final String msgDefaultFlags = SimplyVanish.msgLabel + ChatColor.GRAY + "Flags are at default values.";

    public static final Stats stats = new Stats(msgLabel.trim() + "[STATS]");
    public static final int statsUpdateVanishState = stats.getNewId("UpdateVanishState");
    public static final int statsVanish = stats.getNewId("Vanish");
    public static final int statsReappear = stats.getNewId("Reappear");
    public static final int statsSetFlags = stats.getNewId("SetFlags");
    public static final int statsSave = stats.getNewId("SaveData");

    private static TaskChainFactory taskChainFactory;

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }

    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }

    static {
        stats.setLogStats(false);
    }

    /**
     * Constructor: set some default configuration values.
     */
    public SimplyVanish() {
    }

    @Override
    public void onLoad() {
        INSTANCE = this;
        try {
            //core.getHookUtil().addOnLoadHook(new ProtocolLibHook(this));
        } catch (Exception t) {
            //Utils.warn(t);
        }
    }

    @Override
    public void onDisable() {
        if (core.getSettings().saveVanished) {
            core.doSaveVanished();
        }
        core.setEnabled(false);
        // TODO: maybe let all players see each other again?
        Utils.info("Disabled.");
    }

    @Override
    public void onEnable() {
        core = new SimplyVanishCore(this);

        taskChainFactory = BukkitTaskChainFactory.create(this);
        taskChainFactory.setDefaultErrorHandler((e, task) -> {
            final Class<?> aClass = task.getClass();
            final String msg = "Task Exception: " + aClass.getName();
            final Method method = aClass.getEnclosingMethod();
            if (method != null) {
                Utils.severe(msg + " in method " + method.getDeclaringClass().getName() + "::" + method.getName());
            } else {
                Utils.severe(msg);
            }
            TaskChain<?> chain = TaskChain.getCurrentChain();
            Utils.severe("Current Action Index was: " + chain.getCurrentActionIndex());
            Utils.severe(e);
        });

        core.setVanishedFile(new File(getDataFolder(), "vanished.dat"));
        core.getHookUtil().removeAllHooks();
        // load settings
        this.reloadSettings(); // will also load vanished players
        // register events:
        PluginManager pm = this.getServer().getPluginManager();
        for (Listener listener : new Listener[]{
                new AttackListener(core),
                new ChatListener(core),
                new CoreListener(core),
                new DamageListener(core),
                new DropListener(core),
                new InteractListener(core),
                new PickupListener(core),
                new TargetListener(core),
        }) {
            pm.registerEvents(listener, this);
        }
        // commands
        commandManager = new SimplyVanishCommandManager(this, core);
        commandManager.enableUnstableAPI("help");
        commandManager.registerCommand(new VanTellCommand(core));
        commandManager.registerCommand(new VanishCommand(core));
        commandManager.registerCommand(new ReappearCommand(core));
        commandManager.registerCommand(new ToggleVanishCommand(core));
        commandManager.registerCommand(new SimplyVanishCommand(core));
        commandManager.registerCommand(new VanFlagCommand(core));
        commandManager.registerCommand(new VanGodCommand(core));
        commandManager.registerCommand(new VanUngodCommand(core));

        // finished enabling.
        core.setEnabled(true);
        core.addStandardHooks();
        // just in case quadratic time checking:
        try {
            updateAllPlayers();
        } catch (Exception t) {
            Utils.severe("Failed to update players in onEnable (scheduled for next tick), are you using reload?", t);
            getServer().getScheduler().scheduleSyncDelayedTask(this, this::updateAllPlayers);
        }
        Utils.info("Enabled");
    }

    /**
     * Quadratic time.
     */
    private void updateAllPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            core.updateVanishState(player);
            // TODO: this remains a source of trouble when reloading !
        }
    }

    /**
     * Force reloading the config.
     */
    public void reloadSettings() {
        Server server = getServer();
        BukkitScheduler scheduler = server.getScheduler();
        scheduler.cancelTasks(this);
        CompatConfig config = CompatConfigFactory.getConfig(new File(getDataFolder(), "config.yml"));
        final Path path;
        //		if (config.setPathSeparatorChar('/')){
        //			path = new Path('/');
        //		} else{
        //			// This would  render some things inoperable (permissions with dot as keys).
        path = new Path('.');
        //		}
        boolean changed = false;
        Settings settings = new Settings();
        try {
            config.load();
            changed = Settings.addDefaults(config, path);
            settings.applyConfig(config, path);
        } catch (Exception t) {
            Utils.severe("Failed to load the configuration, continue with default settings. ", t);
            settings = new Settings();
        }
        core.setSettings(settings);
        if (changed) {
            config.save(); // TODO: maybe check for changes, somehow ?
        }
        if (settings.saveVanished) {
            core.loadVanished();
        }
        if (settings.pingEnabled) {
            final long period = Math.max(settings.pingPeriod / 50, 200);
            scheduler.scheduleSyncRepeatingTask(this, core::onNotifyPing, period, period);
        }
        if (settings.saveVanishedInterval > 0) {
            final long period = settings.saveVanishedInterval / 50;
            scheduler.scheduleSyncRepeatingTask(this, core::doSaveVanished, period, period);
        }
        // Load plugins (permissions!):
        PluginManager pm = server.getPluginManager();
        for (String plgName : settings.loadPlugins) {
            Plugin plg = pm.getPlugin(plgName);
            if (plg != null && !plg.isEnabled()) {
                pm.enablePlugin(plg);
            }
        }
    }

    /**
     * API
     *
     * @param player
     * @param vanished success
     */
    public boolean setVanished(@NotNull Player player, boolean vanished) {
        return setVanished(player.getName(), player.getUniqueId(), vanished);
    }

    /**
     * API
     *
     * @param name
     * @param uuid
     * @param vanished success
     */
    public boolean setVanished(@NotNull String name, @NotNull UUID uuid, boolean vanished) {
        if (!core.isEnabled()) {
            return false;
        }
        return core.setVanished(name, uuid, vanished);
    }

    /**
     * API
     *
     * @param player
     * @return
     */
    public boolean isVanished(@NotNull Player player) {
        if (!core.isEnabled()) {
            return false;
        } else {
            return core.isVanished(player.getName(), player.getUniqueId());
        }
    }

    /**
     * API
     *
     * @param uuid
     * @return
     */
    public boolean isVanished(@NotNull UUID uuid) {
        if (!core.isEnabled()) {
            return false;
        } else {
            return core.isVanished(uuid);
        }
    }

    /**
     * API
     *
     * @param name
     * @return
     */
    @Deprecated
    public boolean isVanished(@NotNull String name) {
        if (!core.isEnabled()) {
            return false;
        } else {
            return core.isVanished(name);
        }
    }

    /**
     * API
     * Get a new Set containing the lower case names of Players to be vanished.<br>
     * These are not necessarily online.<br>
     *
     * @return
     * @deprecated The method signature will most likely change to Collection or List.
     */
    @Deprecated
    public @NotNull Set<String> getVanishedPlayers() {
        if (!core.isEnabled()) {
            return new HashSet<>();
        } else {
            return core.getVanishedPlayersNames();
        }
    }

    /**
     * Convenience method, get default VanishConfig (copy), for checking flags.<br>
     * Later on this might be useful, because default flags might be configurable.
     *
     * @return
     */
    public @NotNull VanishConfig getDefaultVanishConfig() {
        return new VanishConfig();
    }

    /**
     * The create flag does in this case not force to store the configuration internally. To force that you have to use setVanishConfig.
     * API
     *
     * @param player
     * @param create
     * @return A clone of the VanishConfig.
     */
    @Contract("_, true -> !null")
    public VanishConfig getVanishConfig(@NotNull Player player, boolean create) {
        return getVanishConfig(player.getName(), player.getUniqueId(), create);
    }

    /**
     * The create flag does in this case not force to store the configuration internally. To force that you have to use setVanishConfig.
     * API
     *
     * @param uuid
     * @return A clone of the VanishConfig.
     */
    @Contract("_, true -> !null")
    public VanishConfig getVanishConfig(@NotNull UUID uuid, boolean create) {
        VanishConfig config = core.getVanishConfig(uuid);
        if (config == null) {
            return create ? getDefaultVanishConfig() : null;
        } else {
            return config.clone();
        }
    }

    /**
     * The create flag does in this case not force to store the configuration internally. To force that you have to use setVanishConfig.
     * API
     *
     * @param name
     * @param uuid
     * @param create
     * @return A clone of the VanishConfig.
     */
    @Contract("_, _, true -> !null")
    public VanishConfig getVanishConfig(@NotNull String name, @NotNull UUID uuid, boolean create) {
        VanishConfig cfg = core.getVanishConfig(name, uuid, false);
        if (cfg == null) {
            return create ? getDefaultVanishConfig() : null;
        } else {
            return cfg.clone();
        }
    }

    /**
     * The create flag does in this case not force to store the configuration internally. To force that you have to use setVanishConfig.
     * API
     *
     * @param name
     * @param create
     * @return A clone of the VanishConfig.
     */
    @Deprecated
    @Contract("_, true -> !null")
    public VanishConfig getVanishConfig(@NotNull String name, boolean create) {
        VanishConfig cfg = core.getVanishConfig(name, false);
        if (cfg == null) {
            if (create) {
                return getDefaultVanishConfig();
            } else {
                return null;
            }
        } else {
            return cfg.clone();
        }
    }

    /**
     * Set the VanishConfig for the player, silently (no notifications), issues saving the configs.<br>
     * This actually will create a new config and apply changes from the given one.<br>
     * If update is true, this will bypass hooks and events.
     *
     * @param name
     * @param uuid
     * @param cfg
     * @param update
     */
    public void setVanishConfig(@NotNull String name, @NotNull UUID uuid, @NotNull VanishConfig cfg, boolean update) {
        setVanishConfig(name, uuid, cfg, update, false);
    }

    /**
     * Set the VanishConfig for the player, silently (no notifications), issues saving the configs.<br>
     * This actually will create a new config and apply changes from the given one.<br>
     * If update is true, this will bypass hooks and events.
     *
     * @param name
     * @param cfg
     * @param update
     */
    @Deprecated
    public void setVanishConfig(@NotNull String name, @NotNull VanishConfig cfg, boolean update) {
        setVanishConfig(name, cfg, update, false);
    }

    /**
     * Set the VanishConfig for the player, with optional notifications, if the player is online, does issue saving the configs.<br>
     * This actually will create a new config and apply changes from the given one.<br>
     * If update is true, this will bypass hooks and events.
     *
     * @param name
     * @param uuid
     * @param cfg
     * @param update
     * @param message
     */
    public VanishConfig setVanishConfig(@NotNull String name, @NotNull UUID uuid, @NotNull VanishConfig cfg, boolean update, boolean message) {
        return core.setVanishConfig(name, uuid, cfg, update, message);
    }

    /**
     * Set the VanishConfig for the player, with optional notifications, if the player is online, does issue saving the configs.<br>
     * This actually will create a new config and apply changes from the given one.<br>
     * If update is true, this will bypass hooks and events.
     *
     * @param name
     * @param cfg
     * @param update
     * @param message
     */
    @Deprecated
    public VanishConfig setVanishConfig(@NotNull String name, @NotNull VanishConfig cfg, boolean update, boolean message) {
        return core.setVanishConfig(name, cfg, update, message);
    }

    /**
     * Force an update of who sees who for this player, without notification, as if SimplyVanish would call it internally.<br>
     *
     * @param player
     * @return false, if the action was prevented by a hook, true otherwise.
     */
    public boolean updateVanishState(@NotNull Player player) {
        return core.updateVanishState(player, false); // Mind the difference of flag to core.updateVanishState(Player).
    }

    /**
     * Force an update of who sees who for this player, without notification.<br>
     *
     * @param player
     * @param hookId To identify who calls this, 0 = as if SimplyVanish called it.
     * @return false, if the action was prevented by a hook, true otherwise.
     */
    public boolean updateVanishState(@NotNull Player player, int hookId) {
        return core.updateVanishState(player, false, hookId);
    }

    /**
     * Force an update of who sees who for this player, with optional notification messages, as if SimplyVanish would call it internally.<br>
     *
     * @param player
     * @param message If to send notifications and state messages.
     * @return false, if the action was prevented by a hook, true otherwise.
     */
    public boolean updateVanishState(@NotNull Player player, boolean message) {
        return core.updateVanishState(player, message);
    }

    /**
     * Force an update of who sees who for this player, with optional notification messages.<br>
     *
     * @param player
     * @param message
     * @param hookId  To identify who calls this, 0 = as if SimplyVanish called it.
     * @return false, if the action was prevented by a hook, true otherwise.
     */
    public boolean updateVanishState(@NotNull Player player, boolean message, int hookId) {
        return core.updateVanishState(player, message, hookId);
    }

    /**
     * Get a new hook id to be passed for certain calls, to allow knowing if your own code called updateVanishState.
     * API
     *
     * @return
     */
    public int getNewHookId() {
        return core.getHookUtil().getNewHookId();
    }

    /**
     * API
     *
     * @param hook
     * @return If one was already present.
     */
    public boolean addHook(@NotNull Hook hook) {
        return core.getHookUtil().addHook(hook);
    }

    /**
     * Get a hook if registered, the name must exactly match.
     *
     * @param name
     * @return Hook or null, if not registered.
     */
    @Deprecated
    public Hook getRegisteredHook(@NotNull String name) {
        return core.getHookUtil().getHook(name);
    }

    /**
     * Listeners can not be removed yet.
     * API
     *
     * @param hook
     * @return If one was already present.
     */
    public boolean removeHook(@NotNull Hook hook) {
        return core.getHookUtil().removeHook(hook);
    }

    /**
     * Listeners can not be removed yet.
     * API
     *
     * @param hookName
     * @return If one was already present.
     */
    public boolean removeHook(@NotNull String hookName) {
        return core.getHookUtil().removeHook(hookName);
    }

    /**
     * Respects allow-ops, superperms and fake-permissions configuration entries.
     *
     * @param sender
     * @param perm
     * @return
     */
    public boolean hasPermission(@NotNull CommandSender sender, @NotNull String perm) {
        return core.hasPermission(sender, perm);
    }

    /**
     * Convenience method used internally.
     *
     * @return
     */
    @NotNull
    public static SimplyVanish getInstance() {
        return INSTANCE;
    }

}
