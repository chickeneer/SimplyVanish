package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.jetbrains.annotations.NotNull;

public final class DropListener implements Listener {
    private final SimplyVanishCore core;

    public DropListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOW)
    void onItemDrop(PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return;
        }
        if (!cfg.vanished.state) {
            return;
        }
        if (!cfg.drop.state) {
            event.setCancelled(true);
        }
    }

}
