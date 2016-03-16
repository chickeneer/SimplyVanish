package me.asofold.bpl.simplyvanish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import me.asofold.bpl.simplyvanish.api.events.SimplyVanishStateEvent;
import me.asofold.bpl.simplyvanish.api.hooks.impl.DisguiseCraftHook;
import me.asofold.bpl.simplyvanish.api.hooks.impl.DynmapHook;
import me.asofold.bpl.simplyvanish.api.hooks.impl.Essentials2Hook;
import me.asofold.bpl.simplyvanish.config.Settings;
import me.asofold.bpl.simplyvanish.config.VanishConfig;
import me.asofold.bpl.simplyvanish.util.HookUtil;
import me.asofold.bpl.simplyvanish.util.Panic;
import me.asofold.bpl.simplyvanish.util.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;


/**
 * Core methods for vanish/reappear.
 * @author mc_dev
 *
 */
public class SimplyVanishCore{
	
	/**
	 * Vanished players.
	 */
	private final Map<String, VanishConfig> vanishConfigs = Collections.synchronizedMap(new HashMap<String, VanishConfig>(20, 0.5f));
	
	private final HookUtil hookUtil = new HookUtil();
	
	/**
	 * Flag for if the plugin is enabled.
	 */
	private boolean enabled = false;
	
	private Settings settings = new Settings();
	
	private long tsSave = 0;
	private int saveTaskId = -1;
	/*
	 * File to save vanished players to.
	 */
	private File vanishedFile = null;
	
	private SimplyVanish plugin;
	
	/**
	 * Only has relevance for static access by Plugin.
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * Only for static access by plugin.
	 * @return
	 */
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setSettings(Settings settings){
		this.settings = settings;
	}
	
	public SimplyVanish getPlugin() {
		return plugin;
	}

	public void setPlugin(SimplyVanish plugin) {
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
	public void onSaveVanished(){
		if (settings.saveVanishedDelay >= 0){
			if (System.currentTimeMillis() - tsSave > settings.saveVanishedDelay){
				// Delay has expired.
				if (saveTaskId != -1) saveTaskId = -1; // Do not cancel anything, presumably that is done.
				doSaveVanished();
			} else{
				// Within delay time frame, schedule new task (unless already done):
				BukkitScheduler sched = Bukkit.getServer().getScheduler();
				if (saveTaskId != -1 && sched.isQueued(saveTaskId)) return;
				saveTaskId = sched.scheduleSyncDelayedTask(plugin, new Runnable(){
					@Override
					public void run() {
						onSaveVanished(); // Check if save is necessary.
					}
				}, 1+(settings.saveVanishedDelay/50));
				if (saveTaskId == -1) doSaveVanished(); // force save if scheduling failed.
			}
		}
		else doSaveVanished(); // Delay is not used.
	}
	
	/**
	 * Force save vanished names to file, does NOT update states (!).
	 */
	public void doSaveVanished(){
		long ns = System.nanoTime();
		tsSave = System.currentTimeMillis();
		File file = getVanishedFile();
		if ( file==null){
			Utils.warn("Can not save vanished players: File is not set.");
			return;
		}
		if (!createFile(file, "vanished players")) return;
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(file));
			writer.write("\n"); // to write something at least.
			Map<String, VanishConfig> copyMap = new HashMap<String,VanishConfig>(vanishConfigs.size());
			synchronized(vanishConfigs){
				for (Entry<String, VanishConfig> entry : vanishConfigs.entrySet()){
					copyMap.put(entry.getKey(), entry.getValue().clone());
				}
			}
			for (Entry<String, VanishConfig> entry : copyMap.entrySet()){
				String n = entry.getKey();
				VanishConfig cfg = entry.getValue();
				if (cfg.needsSave()){
					writer.write(n);
					writer.write(cfg.toLine());
					writer.write("\n");
				}
				cfg.changed = false;
			}
		} 
		catch (IOException e) {
			Utils.warn("Can not save vanished players: "+e.getMessage());
		}
		finally{
			if ( writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}
		SimplyVanish.stats.addStats(SimplyVanish.statsSave, System.nanoTime()-ns);
	}
	
	/**
	 * Load vanished names from file.<br>
	 *  This does not update vanished states!<br>
	 *  Assumes each involved VanishConfig to be changed by loading.
	 */
	public void loadVanished(){
		synchronized(vanishConfigs){
			doLoadVanished();
		}
	}
	
	private void doLoadVanished() {
		File file = getVanishedFile();
		if ( file == null){
			Utils.warn("Can not load vanished players: File is not set.");
			return;
		}
		if (!createFile(file, "vanished players")) return;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader( new FileReader(file));
			String line = "";
			while ( line != null){
				String n = line.trim().toLowerCase();
				if (!n.isEmpty()){
					if (n.startsWith("nosee:") && n.length()>6){
						// kept for compatibility:
						n = n.substring(7).trim();
						if (n.isEmpty()) continue;
						VanishConfig cfg = getVanishConfig(n, true);
						cfg.set(cfg.see, false);
					}
					else{
						String[] split = n.split(" ");
						n = split[0].trim().toLowerCase();
						if (n.isEmpty()) continue;
						VanishConfig cfg = getVanishConfig(n, true);
						cfg.readFromArray(split, 1, true);
					}
				}
				line = reader.readLine();
			}
		} 
		catch (IOException e) {
			Utils.warn("Can not load vanished players: "+e.getMessage());
		}
		finally{
			if ( reader != null)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * Create if not exists.
	 * @param file
	 * @param tag
	 * @return if exists now.
	 */
	public boolean createFile(File file, String tag){
		if ( !file.exists() ){
			try {
				if ( file.createNewFile()) return true;
				else{
					Utils.warn("Could not create "+tag+" file.");
				}
			} catch (IOException e) {
				Utils.warn("Could not create "+tag+" file: "+e.getMessage());
				return false;
			}
		}
		return true;
	}
	
	public File getVanishedFile(){
		return vanishedFile;
	}
	
	public void setVanishedFile(File file) {
		vanishedFile = file;
	}
	
	/**
	 * Adjust state of player to vanished, message player.
	 * @param player
	 */
	public void doVanish(Player player) {
		doVanish(player, true);
	}
	
	/**
	 * Adjust state of player to vanished.
	 * @param player
	 * @param message If to message players.
	 */
	public void doVanish(final Player player, final boolean message) {
		final long ns = System.nanoTime();
		final String name = player.getName();
		boolean was = !addVanishedName(name);
		String fakeQuit = null;
		final VanishConfig vcfg = getVanishConfig(name, true);
		if (settings.sendFakeMessages && !settings.fakeQuitMessage.isEmpty() && !vcfg.online.state) {
			fakeQuit = settings.fakeQuitMessage.replaceAll("%name", name);
			fakeQuit = fakeQuit.replaceAll("%displayname", player.getDisplayName());
		}
		final String msgNotify = SimplyVanish.msgLabel+ChatColor.GREEN+name+ChatColor.GRAY+" vanished.";
		for (final Player other : Bukkit.getServer().getOnlinePlayers()){
			if (other.getName().equals(name)) continue;
			final boolean shouldSee = shouldSeeVanished(other);
			final boolean notify = settings.notifyState && hasPermission(other, settings.notifyStatePerm);
			if (other.canSee(player)){
				if (!shouldSee) {
					hidePlayer(player, other); 
				}
				if (notify){
					if (!was){
						if (message) {
							other.sendMessage(msgNotify);
						}
					}
				} else if (!shouldSee){
					if (fakeQuit != null) {
						other.sendMessage(fakeQuit);
					}
				}
			} else{
				if (shouldSee) {
					showPlayer(player, other); // added as consistency check
				}
				if (!was && notify){
					if (message) {
						other.sendMessage(msgNotify);
					}
				}
			}
		}
		if (message && vcfg.notify.state) {
			player.sendMessage(was?SimplyVanish.msgStillInvisible:SimplyVanish.msgNowInvisible);
		}
		SimplyVanish.stats.addStats(SimplyVanish.statsVanish, System.nanoTime()-ns);
	}

	/**
	 * Adjust state of player to not vanished.
	 * @param player
	 *  @param message If to send messages.
	 */
	public void doReappear(final Player player, final boolean message) {
		final long ns = System.nanoTime();
		final String name = player.getName();
		final boolean was = removeVanishedName(name);
		String fakeJoin = null;
		final VanishConfig vcfg = getVanishConfig(name, true);
		if (settings.sendFakeMessages && !settings.fakeJoinMessage.isEmpty() &&!vcfg.online.state) {
			fakeJoin = settings.fakeJoinMessage.replaceAll("%name", name);
			fakeJoin = fakeJoin.replaceAll("%displayname", player.getDisplayName());
		}
		final String msgNotify = SimplyVanish.msgLabel+ChatColor.RED+name+ChatColor.GRAY+" reappeared.";
		for (final Player other : Bukkit.getServer().getOnlinePlayers()){
			if (other.getName().equals(name)) {
				continue;
			}
			boolean notify = settings.notifyState && hasPermission(other, settings.notifyStatePerm);
			if (!other.canSee(player)){
				showPlayer(player, other);
				if (notify){
					if (message) {
						other.sendMessage(msgNotify);
					}
				} else if (!shouldSeeVanished(other)){
					if (fakeJoin != null) {
						other.sendMessage(fakeJoin);
					}
				}
			} 
			else{
				// No need to adjust visibility.
				if (was && notify){
					if (message) {
						other.sendMessage(msgNotify);
					}
				}
			}
		}
		if (message && vcfg.notify.state) {
			player.sendMessage(SimplyVanish.msgLabel+ChatColor.GRAY+"You are "+(was?"now":"still")+" "+ChatColor.RED+"visible"+ChatColor.GRAY+" to everyone!");
		}
		SimplyVanish.stats.addStats(SimplyVanish.statsReappear, System.nanoTime()-ns);
	}
	
	/**
	 * Public access method for vanish/reappear.<br>
	 * This will call hooks.<br>
	 * This can also be used to force a state update, though updateVanishState should be sufficient.
	 * 
	 * @param playerName
	 * @param vanished
	 * @return 
	 */
	public boolean setVanished(String playerName, boolean vanished) {
		playerName = playerName.toLowerCase();
		boolean was = isVanished(playerName);
		// call event:
		SimplyVanishStateEvent svEvent = new SimplyVanishStateEvent(playerName, was, vanished);
		Bukkit.getServer().getPluginManager().callEvent(svEvent);
		if (svEvent.isCancelled()){
			// no state update !
			return false;
		}
		vanished = svEvent.getVisibleAfter();
		// TODO
		// call hooks
		if (vanished) hookUtil.callBeforeVanish(playerName);
		else hookUtil.callBeforeReappear(playerName);

		// Do vanish or reappear:
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player != null){
			// The simple but costly part.
			if ( vanished) doVanish(player, true);
			else doReappear(player, true);
		}
		else{
			// Now very simple (lower-case names).
			if (vanished) addVanishedName(playerName);
			else removeVanishedName(playerName); 
		}
		// call further hooks:
		if (vanished) hookUtil.callAfterVanish(playerName);
		else hookUtil.callAfterReappear(playerName);
		return true;
	}
	
	/**
	 * Heavy update for who can see this player and whom this player can see.<br>
	 * This is for internal calls (hookId 0).<br>
	 * This will send notification messages.
	 * @param player
	 */
	public boolean updateVanishState(Player player){
		return updateVanishState(player, true);
	}
	
	/**
	 * Heavy update for who can see this player and whom this player can see and other way round.
	 * This is for internal calls (hookId 0).<br>
	 * @param player
	 * @param message If to message the player.
	 */
	public boolean updateVanishState(final Player player, final boolean message){
		return updateVanishState(player, message, 0);
	}
	
	/**
	 * Heavy update for who can see this player and whom this player can see and other way round.
	 * @param player
	 * @param message If to message the player.
	 * @param hookId Id of the caller (0  = SimplyVanish, >0 = some other registered hook or API call)
	 */
	public boolean updateVanishState(final Player player, final boolean message, int hookId){
		final long ns = System.nanoTime();
		final String playerName = player.getName();
		if (!hookUtil.allowUpdateVanishState(player, hookId)) {
			// TODO: either just return or still do messaging ?
			if (isVanished(playerName)) addVanishedName(playerName);
			else removeVanishedName(playerName);
			SimplyVanish.stats.addStats(SimplyVanish.statsUpdateVanishState, System.nanoTime()-ns);
			return false;
		}
		final Server server = Bukkit.getServer();
		final Collection<? extends Player> players = server.getOnlinePlayers();
		final boolean shouldSee = shouldSeeVanished(player);
		final boolean was = isVanished(playerName);
		// Show or hide other players to player:
		for (final Player other : players){
			if (shouldSee||!isVanished(other.getName())){
				if (!player.canSee(other)) showPlayer(other, player);
			} 
			else if (player.canSee(other)) hidePlayer(other, player);
			if (!was && !other.canSee(player)) showPlayer(player, other);   
		}
		if (was) doVanish(player, message); // remove: a) do not save 2x b) people will get notified.	
		else removeVanishedName(playerName);
		SimplyVanish.stats.addStats(SimplyVanish.statsUpdateVanishState, System.nanoTime()-ns);
		return true;
	}
	
	/**
	 * Only set the flags, no save.
	 * TODO: probably needs a basic mix-in permission to avoid abuse (though that would need command spam).
	 * @param playerName
	 * @param args
	 * @param startIndex Start parsing flags from that index on.
	 * @param sender For performing permission checks.
	 * @param hasBypass Has some bypass permission (results in no checks)
	 * @param other If is sender is other than name
	 * @param save If to save state.
	 */
	public void setFlags(String playerName, String[] args, int startIndex, CommandSender sender, boolean hasBypass, boolean other, boolean save) {
		long ns = System.nanoTime();
		playerName = playerName.trim().toLowerCase();
		if (playerName.isEmpty()) return;
		final String permBase =  "simplyvanish.flags.set."+(other?"other":"self"); // bypass permission
		if (!hasBypass) hasBypass = hasPermission(sender, permBase);
		VanishConfig cfg = getVanishConfig(playerName, false);
		boolean hasSomePerm = hasBypass; // indicates that the player has any permission at all.
		if (cfg == null) cfg = new VanishConfig();
		boolean hasClearFlag = false;
		List<String[]> applySets = new LinkedList<String[]>();
		for ( int i = startIndex; i<args.length; i++){
			final String arg = args[i].trim().toLowerCase();
			if (arg.isEmpty()) continue;
			if (arg.charAt(0) == '*'){
				String name = VanishConfig.getMappedFlagName(arg);
				if ( name.equals("clear")){
					hasClearFlag = true;
				}
				else if (settings.flagSets.containsKey(name)){
					String[] set = settings.flagSets.get(name);
					applySets.add(set);
					for ( String x : set){
						if (VanishConfig.getMappedFlagName(x).equals("clear")) hasClearFlag = true;
					}
				}
			}
		}
		VanishConfig newCfg;
		if (hasClearFlag){
			newCfg = new VanishConfig();
			newCfg.set("vanished", cfg.get("vanished"));
		}
		else newCfg = cfg.clone();
		// flag sets:
		for (String[] temp : applySets){
			newCfg.readFromArray(temp, 0, false);
		}
		newCfg.readFromArray(args, startIndex, false);
		
		List<String> changes = cfg.getChanges(newCfg);
		
		// Determine permissions and apply valid changes:
		Set<String> missing = new HashSet<String>();
			
		Set<String> ok = new HashSet<String>();
		for ( String fn : changes){
			String name = fn.substring(1);
			if (!hasBypass && !hasPermission(sender, permBase+"."+name)) missing.add(name);
			else{
				hasSomePerm = true;
				ok.add(name);
			}
		}
		
		if (!missing.isEmpty()) Utils.send(sender, SimplyVanish.msgLabel+ChatColor.RED+"Missing permission for flags: "+Utils.join(missing, ", "));
		if (!hasSomePerm){
			// Difficult: might be a player without ANY permission.
			// TODO: maybe check permissions for all flags
			Utils.send(sender, SimplyVanish.msgLabel+ChatColor.DARK_RED+"You can not set these flags.");
			SimplyVanish.stats.addStats(SimplyVanish.statsSetFlags, System.nanoTime()-ns);
			return;
		}
		// if pass:
		putVanishConfig(playerName, cfg); // just to ensure it is there.
		
		// TODO: setflags event !
		
		hookUtil.callBeforeSetFlags(playerName, cfg.clone(), newCfg.clone());
		// Now actually apply changes to he vcfg.
		for (String name : ok){
			cfg.set(name, newCfg.get(name));
		}
		if ( save && cfg.changed && settings.saveVanishedAlways) onSaveVanished();
		Player player = Bukkit.getServer().getPlayerExact(playerName);
		if (player != null){
			updateVanishState(player, false);
			// TODO: what if returns false 
		}
		if (!cfg.needsSave()) removeVanishedName(playerName);
		hookUtil.callAfterSetFlags(playerName);
		SimplyVanish.stats.addStats(SimplyVanish.statsSetFlags, System.nanoTime()-ns);
	}
	
	/**
	 * Show flags for name to sender, or use the senders name, if name is null.
	 * @param sender
	 * @param name
	 */
	public void onShowFlags(CommandSender sender, String name) {
		if ( name == null) name = sender.getName();
		name = name.toLowerCase();
		VanishConfig cfg = getVanishConfig(name, false);
		if (cfg != null){
			if (!cfg.needsSave()) {
				sender.sendMessage(SimplyVanish.msgDefaultFlags);
			}
			else {
				sender.sendMessage(SimplyVanish.msgLabel+ChatColor.GRAY+"Flags("+name+"): " + cfg.toLine());
			}
		}
		else {
			sender.sendMessage(SimplyVanish.msgDefaultFlags);
		}
	}
	
	/**
	 * Show player to canSee.
	 * Delegating method, for the case of other things to be checked.
	 * @param player The player to show.
	 * @param canSee 
	 */
	void showPlayer(Player player, Player canSee){
		if (!Panic.checkInvolved(player, canSee, "showPlayer", settings.noAbort)) return;
		if (!hookUtil.allowShow(player, canSee)) return;
		try{
			canSee.showPlayer(player);
		} catch(Throwable t){
			Utils.severe("showPlayer failed (show "+player.getName()+" to "+canSee.getName()+"): "+t.getMessage());
			t.printStackTrace();
			Panic.onPanic(settings, new Player[]{player, canSee});
		}
	}
	
	/**
	 * Hide player from canNotSee.
	 * Delegating method, for the case of other things to be checked.
	 * @param player The player to hide.
	 * @param canNotSee
	 */
	void hidePlayer(Player player, Player canNotSee){
		if (!Panic.checkInvolved(player, canNotSee, "hidePlayer", settings.noAbort)) return;
		if (!hookUtil.allowHide(player, canNotSee)) return;
		try{
			canNotSee.hidePlayer(player);
		} catch ( Throwable t){
			Utils.severe("hidePlayer failed (hide "+player.getName()+" from "+canNotSee.getName()+"): "+t.getMessage());
			t.printStackTrace();
			Panic.onPanic(settings, new Player[]{player, canNotSee});
		}
	}
	
	public boolean addVanishedName(String name) {
		VanishConfig cfg = getVanishConfig(name, true);
		boolean res = false;
		if (!cfg.vanished.state){
			cfg.set(cfg.vanished, true);
			res = true;
		}
		if (cfg.changed && settings.saveVanishedAlways) onSaveVanished();
		return res;
	}

	/**
	 * 
	 * @param name
	 * @return If the player was vanished.
	 */
	public boolean removeVanishedName(String name) {
		VanishConfig cfg = getVanishConfig(name, false);
		if (cfg==null) return false;
		boolean res = false;
		if (cfg.vanished.state){
			cfg.set(cfg.vanished, false);
			if (!cfg.needsSave()) removeVanishConfig(name);
			res = true;
		}
		if (cfg.changed && settings.saveVanishedAlways) onSaveVanished();
		return res;
	}
	
	/**
	 * Central access point for checking if player has permission and wants to see vanished players.
	 * @param player
	 * @return
	 */
	public final boolean shouldSeeVanished(final Player player) {
		final VanishConfig cfg = getVanishConfig(player.getName(), false);
		if(cfg!=null){
			if (!cfg.see.state) return false;
		}
		return hasPermission(player, "simplyvanish.see-all"); 
	}

	public final boolean isVanished(final String playerName) {
		final VanishConfig cfg = getVanishConfig(playerName, false);
		if (cfg == null) return false;
		else return cfg.vanished.state;
	}

	/**
	 * Lower case names.<br>
	 * Currently iterates over all VanishConfig entries.
	 * @return
	 */
	public Set<String> getVanishedPlayers() {
		Set<String> out = new HashSet<String>();
		synchronized(vanishConfigs){
			for (Entry<String, VanishConfig> entry : vanishConfigs.entrySet()){
				if (entry.getValue().vanished.state) out.add(entry.getKey());
			}
		}
		return out;
	}
	
	public String getVanishedMessage() {
		List<String> sorted = getSortedVanished();
		StringBuilder builder = new StringBuilder();
		builder.append(ChatColor.GOLD+"[VANISHED]");
		Server server = Bukkit.getServer();
		boolean found = false;
		for ( String n : sorted){
			Player player = server.getPlayerExact(n);
			VanishConfig cfg = vanishConfigs.get(n);
			if (!cfg.vanished.state) continue;
			found = true;
			boolean isNosee = !cfg.see.state; // is lower case
			if ( player == null ){
				builder.append(" "+ChatColor.GRAY+"("+n+")");
				if (isNosee) builder.append(ChatColor.DARK_RED+"[NOSEE]");
			}
			else{
				builder.append(" "+ChatColor.GREEN+player.getName());
				if (!hasPermission(player, "simplyvanish.see-all")) builder.append(ChatColor.DARK_RED+"[CANTSEE]");
				else if (isNosee) builder.append(ChatColor.RED+"[NOSEE]");
			}
		}
		if (!found) builder.append(" "+ChatColor.DARK_GRAY+"<none>");
		return builder.toString();
	}
	
	/**
	 * Unlikely that sorted is needed, but anyway.
	 * @return
	 */
	public List<String> getSortedVanished(){
		Collection<String> vanished = getVanishedPlayers();
		List<String> sorted = new ArrayList<String>(vanished.size());
		sorted.addAll(vanished);
		Collections.sort(sorted);
		return sorted;
	}

	public void onNotifyPing() {
		if (!settings.pingEnabled) return;
		Set<String> keys = new HashSet<String>();
		synchronized (vanishConfigs) {
			keys.addAll(vanishConfigs.keySet());
		}
		for (final String name : keys){
			final VanishConfig cfg = vanishConfigs.get(name);
			if (cfg == null) continue;
			final Player player = Bukkit.getPlayerExact(name);
			if (player == null) continue;
			if (!cfg.vanished.state) continue;
			if (!cfg.ping.state || !cfg.notify.state) continue;
			player.sendMessage(SimplyVanish.msgNotifyPing);
		}
	}
	
	/**
	 * Get a VanishConfig, create it if necessary.<br>
	 * (Might be from vanished, parked, or new thus put to parked).
	 * @param lcName
	 * @return
	 */
	public final VanishConfig getVanishConfig(final String name, final boolean create){
		final String lcName = name.toLowerCase();
		final VanishConfig cfg = vanishConfigs.get(lcName);
		if (cfg != null) return cfg;
		else if (!create) return null;
		else{
			final VanishConfig newCfg = new VanishConfig();
			vanishConfigs.put(lcName, newCfg);
			return newCfg;	
		}
	}
	
	private final void putVanishConfig(final String name, final VanishConfig cfg) {
		vanishConfigs.put(name.toLowerCase(), cfg);
	}
	
	private final void removeVanishConfig(final String name) {
		vanishConfigs.remove(name.toLowerCase());
	}

	/**
	 * API method.
	 * @param playerName
	 * @param cfg
	 * @param update
	 * @param message
	 */
	public void setVanishConfig(String playerName, VanishConfig cfg,
			boolean update, boolean message) {
		VanishConfig newCfg = new VanishConfig();
		newCfg.setAll(cfg);
		putVanishConfig(playerName, newCfg);
		if (update){
			Player player = Bukkit.getServer().getPlayerExact(playerName);
			if (player != null){
				updateVanishState(player, message);
				// TODO: what if returns false ?
			}
		}
		if (settings.saveVanishedAlways) onSaveVanished();
	}
	
	void addStandardHooks(){
		hookUtil.registerOnLoadHooks();
		try{
			hookUtil.addHook(new DisguiseCraftHook());
		} catch(Throwable t){
		}
		try{
			hookUtil.addHook(new DynmapHook());
		} catch(Throwable t){
		}
		try{
			hookUtil.addHook(new Essentials2Hook());
		} catch(Throwable t){}
	}

	public final boolean hasPermission(final CommandSender sender, final String perm) {
		if (!(sender instanceof Player)) return sender.isOp();
		if (settings.allowOps && sender.isOp()) return true;
		else if (settings.superperms){
			if (sender.hasPermission(perm)) return true;
			else if (sender.hasPermission("simplyvanish.all")) return true; 
		}
		final Set<String> perms = settings.fakePermissions.get(((Player)sender).getName().toLowerCase());
		if (perms == null) return false;
		else if (perms.contains("simplyvanish.all")) return true;
		else return perms.contains(perm.toLowerCase());
	}

	public Settings getSettings() {
		return settings;
	}

	public HookUtil getHookUtil() {
		return hookUtil;
	}

	public int getNewHookId() {
		return hookUtil.getNewHookId();
	}

	public void setGod(String name, boolean god, CommandSender notify) {
		VanishConfig cfg = getVanishConfig(name, true);
		if (god == cfg.god.state){
			if (notify != null) Utils.send(notify, SimplyVanish.msgLabel + ChatColor.GRAY + (name.equalsIgnoreCase(notify.getName())?" You were ":(name + " was "))+(god?"already":"not")+" in "+(god?ChatColor.GREEN:ChatColor.RED)+"god-mode.");
		}
		else{
			cfg.set("god", god);
			if (settings.saveVanishedAlways) onSaveVanished();
			Utils.tryMessage(name, SimplyVanish.msgLabel + ChatColor.GRAY + "You are "+(god?"now":"no longer")+" in "+(god?ChatColor.GREEN:ChatColor.RED)+"god-mode.");
			if (notify != null && !name.equalsIgnoreCase(notify.getName())) Utils.send(notify, SimplyVanish.msgLabel + ChatColor.GRAY + name + " is "+(god?"now":"no longer")+" in "+(god?ChatColor.GREEN:ChatColor.RED)+"god-mode.");
		}
		if (!cfg.needsSave()) removeVanishConfig(name);
	}

}
