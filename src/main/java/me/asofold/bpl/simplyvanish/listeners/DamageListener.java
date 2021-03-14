package me.asofold.bpl.simplyvanish.listeners;

import me.asofold.bpl.simplyvanish.SimplyVanishCore;
import me.asofold.bpl.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public final class DamageListener implements Listener {

    private final SimplyVanishCore core;

    public DamageListener(final SimplyVanishCore core) {
        this.core = core;
    }

    private boolean shouldCancelDamage(final String name) {
        final VanishConfig cfg = core.getVanishConfig(name, false);
        if (cfg == null) {
            return false;
        }
        if (cfg.god.state) {
            return true;
        }
        return cfg.vanished.state && !cfg.damage.state;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    final void onFoodLevel(final FoodLevelChangeEvent event) {
        //		if ( event.isCancelled() ) return;
        final LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        if (event.getFoodLevel() - player.getFoodLevel() >= 0) {
            return;
        }
        if (shouldCancelDamage(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    final void onEntityDamage(final EntityDamageEvent event) {
        //		if ( event.isCancelled() ) return;
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancelDamage(entity.getName())) {
            event.setCancelled(true);
            if (entity.getFireTicks() > 0) {
                entity.setFireTicks(0);
            }
            //			if (event instanceof EntityDamageByEntityEvent){
            //				final EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) event;
            //				final Entity damager = edbee.getDamager();
            //				if (damager instanceof Projectile){
            //					final Projectile p = (Projectile) damager;
            //					// TODO: maybe just remove ? / Settings .
            //					if (p.doesBounce()) p.setBounce(false);
            //					// TODO: test / maybe set the position / velocity
            //					p.teleport(p.getLocation().add(p.getLocation().getDirection().normalize()));
            //					p.setVelocity(p.getVelocity().multiply(-1.0));
            //				}
            //			}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    final void onPotionSplash(final PotionSplashEvent event) {
        try {
            final List<Entity> rem = new LinkedList<>();
            final Collection<LivingEntity> affected = event.getAffectedEntities();
            for (final LivingEntity entity : affected) {
                if (entity instanceof Player) {
                    if (shouldCancelDamage(entity.getName())) {
                        rem.add(entity);
                    }
                }
            }
            if (!rem.isEmpty()) {
                affected.removeAll(rem);
            }
        } catch (Exception t) {
            // ignore (fast addition.)
        }
    }

}
