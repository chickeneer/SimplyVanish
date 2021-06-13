package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.jetbrains.annotations.NotNull;

public final class PickupListener implements Listener {
    private final SimplyVanishCore core;

    public PickupListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onItemPickUp(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        VanishConfig cfg = core.getVanishConfig((Player) entity, false);
        if (cfg == null) {
            return;
        }
        if (!cfg.vanished.state) {
            return;
        }
        if (!cfg.pickup.state) {
            event.setCancelled(true);
        }
    }
}
