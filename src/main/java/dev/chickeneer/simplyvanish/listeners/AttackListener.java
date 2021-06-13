package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;

public final class AttackListener implements Listener {
    private final SimplyVanishCore core;

    public AttackListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    private boolean shouldCancelAttack(@NotNull Player player) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return false;
        }
        return cfg.vanished.state && !cfg.attack.state;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onEntityDamage(EntityDamageByEntityEvent event) {
        // TODO: maybe integrate with the damage check
        Entity entity = event.getDamager();
        if (entity instanceof Projectile) {
            entity = Utils.getShooterEntity((Projectile) entity);
        }
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancelAttack((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onProjectileLaunch(ProjectileLaunchEvent event) {
        Entity entity = Utils.getShooterEntity(event.getEntity());
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancelAttack((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onShootBow(EntityShootBowEvent event) {
        // not sure about this one.
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancelAttack((Player) entity)) {
            event.setCancelled(true);
        }
    }

}
