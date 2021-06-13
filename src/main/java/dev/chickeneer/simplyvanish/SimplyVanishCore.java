package dev.chickeneer.simplyvanish;

import dev.chickeneer.simplyvanish.api.events.SimplyVanishStateEvent;
import dev.chickeneer.simplyvanish.api.hooks.impl.DynmapHook;
import dev.chickeneer.simplyvanish.api.hooks.impl.LibsDisguisesHook;
import dev.chickeneer.simplyvanish.config.PlayerVanishConfig;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.util.HookUtil;
import dev.chickeneer.simplyvanish.util.Panic;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Core methods for vanish/reappear.
 */
public class SimplyVanishCore {

    /**
     * Vanished players.
     */
    private final Map<UUID, PlayerVanishConfig> uuidVanishConfigs = Collections.synchronizedMap(new HashMap<>(20, 0.5f));
    private final Map<String, PlayerVanishConfig> nameVanishConfigs = Collections.synchronizedMap(new HashMap<>(20, 0.5f));
    private final HookUtil hookUtil = new HookUtil();

    /**
     * Flag for if the plugin is enabled.
     */
    private boolean enabled = false;

    private Settings settings = new Settings();

    private long lastSave = 0;
    private int saveTaskId = -1;
    /**
     * File to save vanished players to.
     */
    private File vanishedFile = null;

    private SimplyVanish plugin;

    /**
     * Only has relevance for static access by Plugin.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Only for static access by plugin.
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setSettings(@NotNull Settings settings) {
        this.settings = settings;
    }

    @Nullable
    public SimplyVanish getPlugin() {
        return plugin;
    }

    public void setPlugin(@Nullable SimplyVanish plugin) {
        this.plugin = plugin;
    }


    //	/**
    //	 * TODO: Unused ?
    //	 * @return
    //	 */
    //	public boolean shouldSave(){
    //		synchronized(vanishConfigs){
    //			for (VanishConfig cfg : vanishConfigs.values()){
    //				if (cfg.changed) return true;
    //			}
    //		}
    //		return false;
    //	}

    /**
     * Might save vanished names to file, checks timestamp, does NOT update states (!).
     */
    public void onSaveVanished() {
        if (settings.saveVanishedDelay >= 0) {
            if (System.currentTimeMillis() - lastSave > settings.saveVanishedDelay) {
                // Delay has expired.
                if (saveTaskId != -1) {
                    saveTaskId = -1; // Do not cancel anything, presumably that is done.
                }
                doSaveVanished();
            } else {
                // Within delay time frame, schedule new task (unless already done):
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                if (saveTaskId != -1 && scheduler.isQueued(saveTaskId)) {
                    return;
                }
                // Check if save is necessary.
                saveTaskId = scheduler.scheduleSyncDelayedTask(plugin, this::onSaveVanished, 1 + (settings.saveVanishedDelay / 50));
                if (saveTaskId == -1) {
                    doSaveVanished(); // force save if scheduling failed.
                }
            }
        } else {
            doSaveVanished(); // Delay is not used.
        }
    }

    /**
     * Force save vanished names to file, does NOT update states (!).
     */
    public void doSaveVanished() {
        long ns = System.nanoTime();
        lastSave = System.currentTimeMillis();
        File file = getVanishedFile();
        if (file == null) {
            Utils.warn("Can not save vanished players: File is not set.");
            return;
        }
        if (!createFile(file, "vanished players")) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("\n"); // to write something at least.
            Map<String, PlayerVanishConfig> copyNameMap = new HashMap<>(nameVanishConfigs.size());
            synchronized (nameVanishConfigs) {
                for (Entry<String, PlayerVanishConfig> entry : nameVanishConfigs.entrySet()) {
                    copyNameMap.put(entry.getKey(), entry.getValue().clone());
                }
            }
            for (Entry<String, PlayerVanishConfig> entry : copyNameMap.entrySet()) {
                String n = entry.getKey();
                PlayerVanishConfig cfg = entry.getValue();
                if (cfg.needsSave()) {
                    writer.write(cfg.toJsonString());
                    writer.write("\n");
                }
                cfg.changed = false;
            }
        } catch (IOException e) {
            Utils.warn("Can not save vanished players: " + e.getMessage());
        }

        SimplyVanish.stats.addStats(SimplyVanish.statsSave, System.nanoTime() - ns);
    }

    /**
     * Load vanished names from file.<br>
     * This does not update vanished states!<br>
     * Assumes each involved VanishConfig to be changed by loading.
     */
    public void loadVanished() {
        synchronized (nameVanishConfigs) {
            synchronized (uuidVanishConfigs) {
                doLoadVanished();
            }
        }
    }

    private void doLoadVanished() {
        File file = getVanishedFile();
        if (file == null) {
            Utils.warn("Can not load vanished players: File is not set.");
            return;
        }
        if (!createFile(file, "vanished players")) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JSONParser parser = new JSONParser();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty()) {
                    if (line.startsWith("{")) {
                        try {
                            PlayerVanishConfig cfg = PlayerVanishConfig.load((JSONObject) parser.parse(line));
                            putVanishConfig(cfg);
                        } catch (ParseException e) {
                            Utils.severe("Json Parse exception: " + line, e);
                        }
                    } else {
                        // Legacy
                        if (line.startsWith("nosee:") && line.length() > 6) {
                            // kept for compatibility:
                            line = line.substring(7).trim();
                            if (line.isEmpty()) {
                                continue;
                            }
                            VanishConfig cfg = getVanishConfig(line, true);
                            cfg.set(cfg.see, false);
                        } else {
                            String[] split = line.split(" ");
                            String name = split[0].trim().toLowerCase();
                            if (name.isEmpty()) {
                                continue;
                            }
                            VanishConfig cfg = getVanishConfig(name, true);
                            cfg.readFromArray(split, 1, true);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Utils.warn("Can not load vanished players: " + e.getMessage());
        }
    }

    /**
     * Create if not exists.
     *
     * @param file
     * @param tag
     * @return if exists now.
     */
    public boolean createFile(@NotNull File file, @NotNull String tag) {
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    return true;
                } else {
                    Utils.warn("Could not create " + tag + " file.");
                    return false;
                }
            } catch (IOException e) {
                Utils.warn("Could not create " + tag + " file: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    @Nullable
    public File getVanishedFile() {
        return vanishedFile;
    }

    public void setVanishedFile(@NotNull File file) {
        vanishedFile = file;
    }

    /**
     * Adjust state of player to vanished, message player.
     *
     * @param player
     */
    public void doVanish(@NotNull Player player) {
        doVanish(player, true);
    }

    /**
     * Adjust state of player to vanished.
     *
     * @param player
     * @param message If to message players.
     */
    public void doVanish(@NotNull Player player, final boolean message) {
        final long ns = System.nanoTime();
        final String name = player.getName();
        boolean was = !addVanished(player.getName(), player.getUniqueId());
        String fakeQuit = null;
        final PlayerVanishConfig vcfg = getVanishConfig(player.getName(), player.getUniqueId(), true);
        if (settings.sendFakeMessages && !settings.fakeQuitMessage.isEmpty() && !vcfg.online.state) {
            fakeQuit = settings.fakeQuitMessage.replaceAll("%name", name);
            fakeQuit = fakeQuit.replaceAll("%displayname", player.getDisplayName());
        }
        String msgNotify = SimplyVanish.msgLabel + ChatColor.GREEN + name + ChatColor.GRAY + " vanished.";
        for (Player other : Bukkit.getServer().getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            final boolean shouldSee = shouldSeeVanished(other);
            final boolean notify = settings.notifyState && hasPermission(other, settings.notifyStatePerm);
            if (other.canSee(player)) {
                if (!shouldSee) {
                    hidePlayer(player, other);
                }
                if (notify) {
                    if (!was) {
                        if (message) {
                            other.sendMessage(msgNotify);
                        }
                    }
                } else if (!shouldSee) {
                    if (fakeQuit != null) {
                        other.sendMessage(fakeQuit);
                    }
                }
            } else {
                if (shouldSee) {
                    showPlayer(player, other); // added as consistency check
                }
                if (!was && notify) {
                    if (message) {
                        other.sendMessage(msgNotify);
                    }
                }
            }
        }
        if (message && vcfg.notify.state) {
            player.sendMessage(was ? SimplyVanish.msgStillInvisible : SimplyVanish.msgNowInvisible);
        }
        SimplyVanish.stats.addStats(SimplyVanish.statsVanish, System.nanoTime() - ns);
    }

    /**
     * Adjust state of player to not vanished.
     *
     * @param player
     * @param message If to send messages.
     */
    public void doReappear(@NotNull Player player, final boolean message) {
        final long ns = System.nanoTime();
        final String name = player.getName();
        final boolean was = removeVanished(player.getName(), player.getUniqueId());
        String fakeJoin = null;
        final VanishConfig vcfg = getVanishConfig(player, true);
        if (settings.sendFakeMessages && !settings.fakeJoinMessage.isEmpty() && !vcfg.online.state) {
            fakeJoin = settings.fakeJoinMessage.replaceAll("%name", name);
            fakeJoin = fakeJoin.replaceAll("%displayname", player.getDisplayName());
        }
        final String msgNotify = SimplyVanish.msgLabel + ChatColor.RED + name + ChatColor.GRAY + " reappeared.";
        for (final Player other : Bukkit.getServer().getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            boolean notify = settings.notifyState && hasPermission(other, settings.notifyStatePerm);
            if (!other.canSee(player)) {
                showPlayer(player, other);
                if (notify) {
                    if (message) {
                        other.sendMessage(msgNotify);
                    }
                } else if (!shouldSeeVanished(other)) {
                    if (fakeJoin != null) {
                        other.sendMessage(fakeJoin);
                    }
                }
            } else {
                // No need to adjust visibility.
                if (was && notify) {
                    if (message) {
                        other.sendMessage(msgNotify);
                    }
                }
            }
        }
        if (message && vcfg.notify.state) {
            player.sendMessage(
                    SimplyVanish.msgLabel + ChatColor.GRAY + "You are " + (was ? "now" : "still") + " " + ChatColor.RED + "visible" + ChatColor.GRAY +
                    " to everyone!");
        }
        SimplyVanish.stats.addStats(SimplyVanish.statsReappear, System.nanoTime() - ns);
    }

    /**
     * Public access method for vanish/reappear.<br>
     * This will call hooks.<br>
     * This can also be used to force a state update, though updateVanishState should be sufficient.
     *
     * @param name
     * @param uuid
     * @param vanished
     * @return
     */
    public boolean setVanished(@NotNull String name, @NotNull UUID uuid, boolean vanished) {
        boolean was = isVanished(name, uuid);
        // call event:
        SimplyVanishStateEvent svEvent = new SimplyVanishStateEvent(name, uuid, was, vanished);
        Bukkit.getServer().getPluginManager().callEvent(svEvent);
        if (svEvent.isCancelled()) {
            // no state update !
            return false;
        }
        vanished = svEvent.getVanishAfter();
        // TODO
        // call hooks
        if (vanished) {
            hookUtil.callBeforeVanish(name, uuid);
        } else {
            hookUtil.callBeforeReappear(name, uuid);
        }

        // Do vanish or reappear:
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // The simple but costly part.
            if (vanished) {
                doVanish(player);
            } else {
                doReappear(player, true);
            }
        } else {
            // Now very simple (lower-case names).
            if (vanished) {
                addVanished(name, uuid);
            } else {
                removeVanished(name, uuid);
            }
        }
        // call further hooks:
        if (vanished) {
            hookUtil.callAfterVanish(name, uuid);
        } else {
            hookUtil.callAfterReappear(name, uuid);
        }
        return true;
    }

    /**
     * Heavy update for who can see this player and whom this player can see.<br>
     * This is for internal calls (hookId 0).<br>
     * This will send notification messages.
     *
     * @param player
     */
    public boolean updateVanishState(@NotNull Player player) {
        return updateVanishState(player, true);
    }

    /**
     * Heavy update for who can see this player and whom this player can see and other way round.
     * This is for internal calls (hookId 0).<br>
     *
     * @param player
     * @param message If to message the player.
     */
    public boolean updateVanishState(@NotNull Player player, final boolean message) {
        return updateVanishState(player, message, 0);
    }

    /**
     * Heavy update for who can see this player and whom this player can see and other way round.
     *
     * @param player
     * @param message If to message the player.
     * @param hookId  Id of the caller (0  = SimplyVanish, >0 = some other registered hook or API call)
     */
    public boolean updateVanishState(@NotNull Player player, final boolean message, int hookId) {
        final long ns = System.nanoTime();
        if (!hookUtil.allowUpdateVanishState(player, hookId)) {
            // TODO: either just return or still do messaging ?
            if (isVanished(player)) {
                addVanished(player.getName(), player.getUniqueId());
            } else {
                removeVanished(player.getName(), player.getUniqueId());
            }
            SimplyVanish.stats.addStats(SimplyVanish.statsUpdateVanishState, System.nanoTime() - ns);
            return false;
        }
        final Server server = Bukkit.getServer();
        final Collection<? extends Player> players = server.getOnlinePlayers();
        final boolean shouldSee = shouldSeeVanished(player);
        final boolean was = isVanished(player);
        // Show or hide other players to player:
        for (final Player other : players) {
            if (shouldSee || !isVanished(other)) {
                if (!player.canSee(other)) {
                    showPlayer(other, player);
                }
            } else if (player.canSee(other)) {
                hidePlayer(other, player);
            }
            if (!was && !other.canSee(player)) {
                showPlayer(player, other);
            }
        }
        if (was) {
            doVanish(player, message); // remove: a) do not save 2x b) people will get notified.
        } else {
            removeVanished(player.getName(), player.getUniqueId());
        }
        SimplyVanish.stats.addStats(SimplyVanish.statsUpdateVanishState, System.nanoTime() - ns);
        return true;
    }


    /**
     * Only set the flags, no save.
     * TODO: probably needs a basic mix-in permission to avoid abuse (though that would need command spam).
     *
     * @param name
     * @param uuid
     * @param args
     * @param startIndex Start parsing flags from that index on.
     * @param sender     For performing permission checks.
     * @param hasBypass  Has some bypass permission (results in no checks)
     * @param other      If is sender is other than name
     * @param save       If to save state.
     */
    public void setFlags(@NotNull String name, @NotNull UUID uuid, @NotNull String[] args, int startIndex, @NotNull CommandSender sender, boolean hasBypass, boolean other, boolean save) {
        long ns = System.nanoTime();
        final String permBase = "simplyvanish.flags.set." + (other ? "other" : "self"); // bypass permission
        if (!hasBypass) {
            hasBypass = hasPermission(sender, permBase);
        }
        PlayerVanishConfig cfg = getVanishConfig(name, uuid, false);
        boolean hasSomePerm = hasBypass; // indicates that the player has any permission at all.
        if (cfg == null) {
            cfg = new PlayerVanishConfig(name, uuid);
        }
        boolean hasClearFlag = false;
        List<String[]> applySets = new LinkedList<>();
        for (int i = startIndex; i < args.length; i++) {
            final String arg = args[i].trim().toLowerCase();
            if (arg.isEmpty()) {
                continue;
            }
            if (arg.charAt(0) == '*') {
                String flagName = VanishConfig.getMappedFlagName(arg);
                if (flagName.equals("clear")) {
                    hasClearFlag = true;
                } else if (settings.flagSets.containsKey(flagName)) {
                    String[] set = settings.flagSets.get(flagName);
                    applySets.add(set);
                    for (String x : set) {
                        if (VanishConfig.getMappedFlagName(x).equals("clear")) {
                            hasClearFlag = true;
                        }
                    }
                }
            }
        }
        VanishConfig newCfg;
        if (hasClearFlag) {
            newCfg = new PlayerVanishConfig(name, uuid);
            newCfg.set("vanished", cfg.get("vanished"));
        } else {
            newCfg = cfg.clone();
        }
        // flag sets:
        for (String[] temp : applySets) {
            newCfg.readFromArray(temp, 0, false);
        }
        newCfg.readFromArray(args, startIndex, false);

        List<String> changes = cfg.getChanges(newCfg);

        // Determine permissions and apply valid changes:
        Set<String> missing = new HashSet<>();

        Set<String> ok = new HashSet<>();
        for (String fn : changes) {
            String flagName = fn.substring(1);
            if (!hasBypass && !hasPermission(sender, permBase + "." + flagName)) {
                missing.add(flagName);
            } else {
                hasSomePerm = true;
                ok.add(flagName);
            }
        }

        if (!missing.isEmpty()) {
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.RED + "Missing permission for flags: " + Utils.join(missing, ", "));
        }
        if (!hasSomePerm) {
            // Difficult: might be a player without ANY permission.
            // TODO: maybe check permissions for all flags
            Utils.send(sender, SimplyVanish.msgLabel + ChatColor.DARK_RED + "You can not set these flags.");
            SimplyVanish.stats.addStats(SimplyVanish.statsSetFlags, System.nanoTime() - ns);
            return;
        }
        // if pass:
        putVanishConfig(cfg); // just to ensure it is there.

        // TODO: setflags event !

        hookUtil.callBeforeSetFlags(name, uuid, cfg.clone(), newCfg.clone());
        // Now actually apply changes to he vcfg.
        for (String flagName : ok) {
            cfg.set(flagName, newCfg.get(flagName));
        }
        if (save && cfg.changed && settings.saveVanishedAlways) {
            onSaveVanished();
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updateVanishState(player, false);
            // TODO: what if returns false
        }
        if (!cfg.needsSave()) {
            removeVanished(name, uuid);
        }
        hookUtil.callAfterSetFlags(name, uuid);
        SimplyVanish.stats.addStats(SimplyVanish.statsSetFlags, System.nanoTime() - ns);
    }

    /**
     * Show flags for uuid to sender.
     *
     * @param sender
     * @param uuid
     */
    public void onShowFlags(@NotNull CommandSender sender, @NotNull UUID uuid) {
        PlayerVanishConfig cfg = getVanishConfig(uuid);
        if (cfg != null) {
            if (!cfg.needsSave()) {
                sender.sendMessage(SimplyVanish.msgDefaultFlags);
            } else {
                sender.sendMessage(SimplyVanish.msgLabel + ChatColor.GRAY + "Flags(" + cfg.getName() + "): " + cfg.toLine());
            }
        } else {
            sender.sendMessage(SimplyVanish.msgDefaultFlags);
        }
    }

    /**
     * Show player to canSee.
     * Delegating method, for the case of other things to be checked.
     *
     * @param player The player to show.
     * @param canSee
     */
    void showPlayer(@NotNull Player player, @NotNull Player canSee) {
        if (!Panic.checkInvolved(player, canSee, "showPlayer", settings.noAbort)) {
            return;
        }
        if (!hookUtil.allowShow(player, canSee)) {
            return;
        }
        try {
            canSee.showPlayer(getPlugin(), player);
        } catch (Exception t) {
            Utils.severe("showPlayer failed (show " + player.getName() + " to " + canSee.getName() + "): " + t.getMessage(), t);
            Panic.onPanic(settings, new Player[]{player, canSee});
        }
    }

    /**
     * Hide player from canNotSee.
     * Delegating method, for the case of other things to be checked.
     *
     * @param player    The player to hide.
     * @param canNotSee
     */
    void hidePlayer(@NotNull Player player, @NotNull Player canNotSee) {
        if (!Panic.checkInvolved(player, canNotSee, "hidePlayer", settings.noAbort)) {
            return;
        }
        if (!hookUtil.allowHide(player, canNotSee)) {
            return;
        }
        try {
            canNotSee.hidePlayer(getPlugin(), player);
        } catch (Exception t) {
            Utils.severe("hidePlayer failed (hide " + player.getName() + " from " + canNotSee.getName() + "): " + t.getMessage());
            t.printStackTrace();
            Panic.onPanic(settings, new Player[]{player, canNotSee});
        }
    }

    public boolean addVanished(@NotNull String name, @NotNull UUID uuid) {
        VanishConfig cfg = getVanishConfig(name, uuid, true);
        boolean res = false;
        if (!cfg.vanished.state) {
            cfg.set(cfg.vanished, true);
            res = true;
        }
        if (cfg.changed && settings.saveVanishedAlways) {
            onSaveVanished();
        }
        return res;
    }

    /**
     * @param name
     * @param uuid
     * @return If the player was vanished.
     */
    public boolean removeVanished(@NotNull String name, @NotNull UUID uuid) {
        PlayerVanishConfig cfg = getVanishConfig(name, uuid, false);
        if (cfg == null) {
            return false;
        }
        boolean res = false;
        if (cfg.vanished.state) {
            cfg.set(cfg.vanished, false);
            if (!cfg.needsSave()) {
                removeVanishConfig(cfg);
            }
            res = true;
        }
        if (cfg.changed && settings.saveVanishedAlways) {
            onSaveVanished();
        }
        return res;
    }

    /**
     * Central access point for checking if player has permission and wants to see vanished players.
     *
     * @param player
     * @return
     */
    public final boolean shouldSeeVanished(@NotNull Player player) {
        final VanishConfig cfg = getVanishConfig(player, false);
        if (cfg != null) {
            if (!cfg.see.state) {
                return false;
            }
        }
        return hasPermission(player, "simplyvanish.see-all");
    }

    public final boolean isVanished(@NotNull Player player) {
        return isVanished(player.getName(), player.getUniqueId());
    }

    public final boolean isVanished(@NotNull String name, @NotNull UUID uuid) {
        VanishConfig cfg = getVanishConfig(name, uuid ,false);
        if (cfg != null) {
            return cfg.vanished.state;
        }
        return false;
    }

    public final boolean isVanished(@NotNull UUID uuid) {
        final VanishConfig cfg = getVanishConfig(uuid);
        if (cfg == null) {
            return false;
        } else {
            return cfg.vanished.state;
        }
    }

    @Deprecated
    public final boolean isVanished(@NotNull String name) {
        final VanishConfig cfg = getVanishConfig(name, false);
        if (cfg == null) {
            return false;
        } else {
            return cfg.vanished.state;
        }
    }

    /**
     * Lower case names.<br>
     * Currently iterates over all VanishConfig entries.
     *
     * @return
     */
    public @NotNull Set<String> getVanishedPlayersNames() { //TODO: What use is this. API use in SimplyVanish class (API)
        Set<String> out = new HashSet<>();
        synchronized (nameVanishConfigs) {
            for (PlayerVanishConfig config : nameVanishConfigs.values()) {
                if (config.vanished.state) {
                    out.add(config.getName());
                }
            }
        }
        return out;
    }

    public @NotNull String getVanishedMessage() {
        List<String> sorted = getSortedVanished();
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GOLD).append("[VANISHED]");
        Server server = Bukkit.getServer();
        boolean found = false;
        for (String n : sorted) {
            Player player = server.getPlayerExact(n);
            VanishConfig cfg = nameVanishConfigs.get(n);
            if (!cfg.vanished.state) {
                continue;
            }
            found = true;
            boolean isNosee = !cfg.see.state; // is lower case
            if (player == null) {
                builder.append(" ").append(ChatColor.GRAY).append("(").append(n).append(")");
                if (isNosee) {
                    builder.append(ChatColor.DARK_RED).append("[NOSEE]");
                }
            } else {
                builder.append(" ").append(ChatColor.GREEN).append(player.getName());
                if (!hasPermission(player, "simplyvanish.see-all")) {
                    builder.append(ChatColor.DARK_RED).append("[CANTSEE]");
                } else if (isNosee) {
                    builder.append(ChatColor.RED).append("[NOSEE]");
                }
            }
        }
        if (!found) {
            builder.append(" ").append(ChatColor.DARK_GRAY).append("<none>");
        }
        return builder.toString();
    }

    /**
     * Unlikely that sorted is needed, but anyway.
     *
     * @return
     */
    public @NotNull List<String> getSortedVanished() {
        Collection<String> vanished = getVanishedPlayersNames();
        List<String> sorted = new ArrayList<>(vanished.size());
        sorted.addAll(vanished);
        Collections.sort(sorted);
        return sorted;
    }

    public void onNotifyPing() {
        if (!settings.pingEnabled) {
            return;
        }
        Set<String> keys;
        synchronized (nameVanishConfigs) {
            keys = new HashSet<>(nameVanishConfigs.keySet());
        }
        for (final String name : keys) {
            final VanishConfig cfg = nameVanishConfigs.get(name);
            if (cfg == null) {
                continue;
            }
            final Player player = Bukkit.getPlayerExact(name);
            if (player == null) {
                continue;
            }
            if (!cfg.vanished.state) {
                continue;
            }
            if (!cfg.ping.state || !cfg.notify.state) {
                continue;
            }
            player.sendMessage(SimplyVanish.msgNotifyPing);
        }
    }
    /**
     * Get a VanishConfig.<br>
     * (Might be from vanished, parked, or new thus put to parked).
     *
     * @param name
     * @return
     */
    @Deprecated
    public final @Nullable PlayerVanishConfig getVanishConfig(@NotNull String name) {
        return getVanishConfig(name, false);
    }
    /**
     * Get a VanishConfig, create it if necessary.<br>
     * (Might be from vanished, parked, or new thus put to parked).
     *
     * @param name
     * @param create
     * @return
     */
    @Deprecated
    @Contract("_, true -> !null")
    public final PlayerVanishConfig getVanishConfig(@NotNull String name, final boolean create) {
        String lowerCaseName = name.toLowerCase();
        PlayerVanishConfig cfg = nameVanishConfigs.get(lowerCaseName);
        if (cfg != null) {
            return cfg;
        } else if (create) {
            PlayerVanishConfig newCfg = new PlayerVanishConfig(name, null);
            nameVanishConfigs.put(lowerCaseName, newCfg);
            return newCfg;
        } else {
            return null;
        }
    }

    public final @Nullable PlayerVanishConfig getVanishConfig(@NotNull UUID uuid) {
        return uuidVanishConfigs.get(uuid);
    }

    @Contract("_, true -> !null")
    public final PlayerVanishConfig getVanishConfig(@NotNull Player player, final boolean create) {
        return getVanishConfig(player.getName(), player.getUniqueId(), create);
    }

    @Contract("_, _, true -> !null")
    public final PlayerVanishConfig getVanishConfig(@NotNull String name, @NotNull UUID uuid, final boolean create) {
        String lowerCaseName = name.toLowerCase();
        PlayerVanishConfig config = uuidVanishConfigs.get(uuid);
        if (config == null) {
            config = nameVanishConfigs.get(name);
        } else {
            // Name validation
            if (!config.getLowerCaseName().equals(lowerCaseName)) {
                config = setVanishConfig(name, uuid, config, false, false);
            }
        }
        if (config != null) {
            return config;
        } else if (create) {
            PlayerVanishConfig newCfg = new PlayerVanishConfig(name, uuid);
            putVanishConfig(newCfg);
            return newCfg;
        } else {
            return null;
        }
    }

    private void putVanishConfig(@NotNull PlayerVanishConfig config) {
        UUID uuid = config.getUuid();
        if (uuid != null) {
            PlayerVanishConfig oldConfig = uuidVanishConfigs.remove(uuid);
            if (oldConfig != null) {
                nameVanishConfigs.remove(oldConfig.getLowerCaseName());
            }
            uuidVanishConfigs.put(uuid, config);
        }
        nameVanishConfigs.put(config.getLowerCaseName(), config);
    }

    private void removeVanishConfig(@NotNull PlayerVanishConfig config) {
        UUID uuid = config.getUuid();
        if (uuid != null) {
            uuidVanishConfigs.remove(uuid);
        }
        nameVanishConfigs.remove(config.getLowerCaseName());
    }

    /**
     * API method.
     *
     * @param name
     * @param uuid
     * @param cfg
     * @param update
     * @param message
     */
    public PlayerVanishConfig setVanishConfig(@NotNull String name, @NotNull UUID uuid, @NotNull VanishConfig cfg, boolean update, boolean message) {
        PlayerVanishConfig newCfg = new PlayerVanishConfig(name, uuid);
        newCfg.setAll(cfg);
        putVanishConfig(newCfg);
        if (update) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                updateVanishState(player, message);
                // TODO: what if returns false ?
            }
        }
        if (settings.saveVanishedAlways) {
            onSaveVanished();
        }
        return newCfg;
    }

    /**
     * API method.
     *
     * @param name
     * @param cfg
     * @param update
     * @param message
     */
    @Deprecated
    public VanishConfig setVanishConfig(@NotNull String name, @NotNull VanishConfig cfg, boolean update, boolean message) {
        PlayerVanishConfig newCfg = new PlayerVanishConfig(name, null);
        newCfg.setAll(cfg);
        putVanishConfig(newCfg);
        if (update) {
            Player player = Bukkit.getServer().getPlayerExact(name);
            if (player != null) {
                updateVanishState(player, message);
                // TODO: what if returns false ?
            }
        }
        if (settings.saveVanishedAlways) {
            onSaveVanished();
        }
        return newCfg;
    }

    void addStandardHooks() {
        hookUtil.registerOnLoadHooks();
        try {
            hookUtil.addHook(new LibsDisguisesHook());
        } catch (Exception ignored) {
        }
        try {
            hookUtil.addHook(new DynmapHook());
        } catch (Exception ignored) {
        }
    }

    public final boolean hasPermission(@NotNull CommandSender sender, @NotNull String perm) {
        if (!(sender instanceof Player)) {
            return sender.isOp();
        }
        if (settings.allowOps && sender.isOp()) {
            return true;
        } else if (settings.superperms) {
            if (sender.hasPermission(perm)) {
                return true;
            } else if (sender.hasPermission("simplyvanish.all")) {
                return true;
            }
        }
        final Set<String> perms = settings.fakePermissions.get(sender.getName().toLowerCase());
        if (perms == null) {
            return false;
        } else if (perms.contains("simplyvanish.all")) {
            return true;
        } else {
            return perms.contains(perm.toLowerCase());
        }
    }

    public @NotNull Settings getSettings() {
        return settings;
    }

    public @NotNull HookUtil getHookUtil() {
        return hookUtil;
    }

    public void setGod(@NotNull String name, @NotNull UUID uuid, boolean god, @Nullable CommandSender notify) {
        PlayerVanishConfig cfg = getVanishConfig(name, uuid, true);
        if (god == cfg.god.state) {
            if (notify != null) {
                Utils.send(notify, SimplyVanish.msgLabel + ChatColor.GRAY + (notify.getName().equalsIgnoreCase(name) ? " You were " : (name + " was ")) +
                                   (god ? "already" : "not") + " in " + (god ? ChatColor.GREEN : ChatColor.RED) + "god-mode.");
            }
        } else {
            cfg.set("god", god);
            if (settings.saveVanishedAlways) {
                onSaveVanished();
            }
            Utils.tryMessage(uuid,
                    SimplyVanish.msgLabel + ChatColor.GRAY + "You are " + (god ? "now" : "no longer") +
                    " in " + (god ? ChatColor.GREEN : ChatColor.RED) + "god-mode.");
            if (notify != null && !notify.getName().equalsIgnoreCase(name)) {
                Utils.send(notify, SimplyVanish.msgLabel + ChatColor.GRAY + name + " is " + (god ? "now" : "no longer") + " in " +
                                   (god ? ChatColor.GREEN : ChatColor.RED) + "god-mode.");
            }
        }
        if (!cfg.needsSave()) {
            removeVanishConfig(cfg);
        }
    }
}
