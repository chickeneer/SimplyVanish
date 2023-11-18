package dev.chickeneer.simplyvanish.api.hooks.impl;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.api.events.SimplyVanishAtLoginEvent;
import dev.chickeneer.simplyvanish.api.events.SimplyVanishStateEvent;
import dev.chickeneer.simplyvanish.api.hooks.AbstractHook;
import dev.chickeneer.simplyvanish.api.hooks.HookListener;
import dev.chickeneer.simplyvanish.api.hooks.HookPurpose;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.util.Formatting;
import dev.chickeneer.simplyvanish.util.Utils;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.events.DisguiseEvent;
import me.libraryaddict.disguise.events.UndisguiseEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public class LibsDisguisesHook extends AbstractHook {

    //	boolean blocked = false; // TODO: consider this.
    private final HookListener listener = new HookListener() {
        @Override
        public boolean unregisterEvents() {
            // TODO: wait for Bukkit ?
            return false;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        void onVisibility(SimplyVanishStateEvent event) {
            if (event.isCancelled()) {
                return;
            }
            if (event.getVanishAfter()) {
                boolean keep = false;
                if (event instanceof SimplyVanishAtLoginEvent) {
                    keep = true;
                }
                onInvisible(event.getPlayerName(), keep);
            } else {
                Player player = event.getPlayer();
                if (player != null && DisguiseAPI.isDisguised(player)) {
                    event.setCancelled(true);
                    // TODO: Consider suppressing if notify=false.
                    if (shouldNotify(player)) {
                        Utils.sendMsg(player, SimplyVanish.MSG_LABEL + "Use " + Formatting.YELLOW + "/undis" + Formatting.GRAY + "guise !");
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        void onDisguise(DisguiseEvent event) {
            if (event.isCancelled()) {
                return;
            }
            if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
                return;
            }
            Player player = (Player) event.getEntity();
            if (SimplyVanish.getInstance().isVanished(player)) {
                if (!SimplyVanish.getInstance().setVanished(player, false)) {
                    event.setCancelled(true); // TODO: something
                    // TODO: Consider suppressing if notify=false.
                    if (shouldNotify(player)) {
                        Utils.sendMsg(player, SimplyVanish.MSG_LABEL + Formatting.ERROR + "Can not disguise (something prevents reappear).");
                    }
                }
            }
        }

        private boolean shouldNotify(@NotNull Player player) {
            final VanishConfig config = SimplyVanish.getInstance().getVanishConfig(player, false);
            if (config == null) {
                return true;
            } else {
                return config.notify.state;
            }
        }

        @SuppressWarnings("unused")
        @EventHandler(priority = EventPriority.MONITOR)
        void onUndisguise(UndisguiseEvent event) {
            if (event.isCancelled()) {
                return;
            }
            if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
                return;
            }
            final Player player = (Player) event.getEntity();
            String name = player.getName();
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(SimplyVanish.getInstance(), () -> {
                Player dummy = Bukkit.getServer().getPlayerExact(player.getName());
                if (dummy != null) {
                    SimplyVanish.getInstance().updateVanishState(dummy, false, hookId);
                }
            });
        }

    };

    /**
     * @param name
     * @param keepDisguise
     */
    public void onInvisible(@NotNull String name, boolean keepDisguise) {
        Player player = Bukkit.getServer().getPlayerExact(name);
        if (player == null) {
            return;
        }
        if (DisguiseAPI.isDisguised(player)) {
            // Disguise and remember disguise.
            DisguiseAPI.undisguiseToAll(player); // TODO: change priorities and check result, act accordingly.
        }
    }

    @Override
    public final boolean allowUpdateVanishState(@NotNull final Player player, final int hookId, boolean isAllowed) {
        if (!isAllowed) {
            return false;
        }
        if (hookId == this.hookId) {
            return true;
        } else {
            return !DisguiseAPI.isDisguised(player);
        }
    }

    @Override
    public boolean allowShow(@NotNull Player player, @NotNull Player canSee, boolean isAllowed) {
        return !DisguiseAPI.isDisguised(player);
    }

    @Override
    public @NotNull String getHookName() {
        return "LibsDisguise";
    }

    @Override
    public @NotNull HookPurpose[] getSupportedMethods() {
        return new HookPurpose[]{HookPurpose.LISTENER, HookPurpose.ALLOW_UPDATE, HookPurpose.ALLOW_SHOW};
    }

    @Override
    public HookListener getListener() {
        return listener;
    }

    public LibsDisguisesHook() {
        LibsDisguises.getInstance().isEnabled(); // to fail in case.
    }

}
