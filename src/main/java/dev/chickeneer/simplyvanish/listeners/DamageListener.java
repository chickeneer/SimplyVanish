package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.jetbrains.annotations.NotNull;

public final class DamageListener implements Listener {
    private final SimplyVanishCore core;

    public DamageListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    private boolean shouldCancelDamage(@NotNull Player player) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return false;
        }
        if (cfg.god.state) {
            return true;
        }
        return cfg.vanished.state && !cfg.damage.state;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onFoodLevel(FoodLevelChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;
        if (event.getFoodLevel() - player.getFoodLevel() >= 0) {
            return;
        }
        if (shouldCancelDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancelDamage((Player) entity)) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPotionSplash(PotionSplashEvent event) {
        event.getAffectedEntities().removeIf(entity -> entity instanceof Player && shouldCancelDamage((Player) entity));
    }

}
