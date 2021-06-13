package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class TargetListener implements Listener {
    private final SimplyVanishCore core;

    public TargetListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        //		if (entity instanceof Tameable){
        //			System.out.println("Target: "  +entity);
        //			// TODO: put some of this into a method.
        //			Tameable tam = (Tameable) entity;
        //			AnimalTamer tamer = tam.getOwner();
        //			if (tam.isTamed() && tamer != null && (tamer instanceof OfflinePlayer)){
        //				// TODO: add heuristic to allow self defense for tameables !
        //				OfflinePlayer player = (OfflinePlayer) tamer;
        //				String name = player.getName();
        //				System.out.println("Target "+ name + " <- "+tam);
        //				VanishConfig cfg = core.getVanishConfig(name, false);
        //				if (cfg.vanished.state || cfg.god.state){
        //					event.setTarget(null);
        //					return;
        //				}
        //			}
        //		}
        Entity target = event.getTarget();
        if (!(target instanceof Player)) {
            return;
        }
        VanishConfig cfg = core.getVanishConfig((Player) target, false);
        if (cfg == null) {
            return;
        }
        if (cfg.vanished.state || cfg.god.state) {
            Settings settings = core.getSettings();
            if (settings.expEnabled && (!cfg.pickup.state || cfg.god.state)) {
                if (entity instanceof ExperienceOrb) {
                    repelExpOrb((Player) target, (ExperienceOrb) entity, settings);
                    event.setCancelled(true);
                    event.setTarget(null);
                    return;
                }
            }
            if (!cfg.target.state || cfg.god.state) {
                event.setTarget(null);
            }
        }
    }

    //	@EventHandler(priority = EventPriority.LOW)
    //	void onEntityTeleport(EntityTeleportEvent event){
    //		Entity entity = event.getEntity();
    //		System.out.println("Teleport: " + entity);
    //		if (entity instanceof Tameable){
    //			Tameable tam = (Tameable) entity;
    //			if (!tam.isTamed()) return;
    //			AnimalTamer tamer = tam.getOwner();
    //			if (tam.isTamed() && tamer != null && (tamer instanceof OfflinePlayer)){
    //				OfflinePlayer player = (OfflinePlayer) tamer;
    //				String name = player.getName();
    //				System.out.println("Teleport "+ name + " <- "+tam);
    //				VanishConfig cfg = core.getVanishConfig(name, false);
    //				if (cfg.vanished.state || cfg.god.state){
    //					if (entity instanceof Animals){
    //						// TODO: maybe set sitting !
    //						((Animals)entity).setTarget(null);
    //					}
    //					event.setCancelled(true);
    //					return;
    //				}
    //			}
    //		}
    //	}

    /**
     * Attempt some workaround for experience orbs:
     * prevent it getting near the player.
     *
     * @param player
     * @param orb
     * @param settings
     */
    void repelExpOrb(@NotNull Player player, @NotNull ExperienceOrb orb, @NotNull Settings settings) {
        Location pLoc = player.getLocation();
        Location oLoc = orb.getLocation();
        Vector dir = oLoc.toVector().subtract(pLoc.toVector());
        double dx = Math.abs(dir.getX());
        double dz = Math.abs(dir.getZ());
        if ((dx == 0.0) && (dz == 0.0)) {
            // Special case probably never happens
            dir.setX(0.001);
        }
        if ((dx < settings.expThreshold) && (dz < settings.expThreshold)) {
            Vector nDir = dir.normalize();
            Vector newV = nDir.clone().multiply(settings.expVelocity);
            newV.setY(0);
            orb.setVelocity(newV);
            if ((dx < settings.expTeleDist) && (dz < settings.expTeleDist)) {
                // maybe oLoc
                orb.teleport(oLoc.clone().add(nDir.multiply(settings.expTeleDist)), TeleportCause.PLUGIN);
            }
            if ((dx < settings.expKillDist) && (dz < settings.expKillDist)) {
                orb.remove();
            }
        }
    }
}
