package dev.chickeneer.simplyvanish.listeners;

import dev.chickeneer.simplyvanish.SimplyVanishCore;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import dev.chickeneer.simplyvanish.inventories.InventoryUtil;
import dev.chickeneer.simplyvanish.util.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.NotNull;

public final class InteractListener implements Listener {
    private final SimplyVanishCore core;

    public InteractListener(@NotNull SimplyVanishCore core) {
        this.core = core;
    }

    private boolean shouldCancel(@NotNull Player player) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return false;
        }
        return cfg.vanished.state && !cfg.interact.state;
    }

    private boolean hasBypass(@NotNull Player player, @NotNull EntityType type) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return false;
        }
        if (!cfg.bypass.state) {
            return false;
        }
        Settings settings = core.getSettings();
        if (!settings.bypassIgnorePermissions && player.hasPermission("simplyvanish.flags.bypass." + (type.toString().toLowerCase()))) {
            return true;
        } else {
            return settings.bypassEntities.contains(type);
        }
    }

    private boolean hasBypass(@NotNull Player player, Material material) {
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return false;
        }
        if (!cfg.bypass.state) {
            return false;
        }
        Settings settings = core.getSettings();
        if (!settings.bypassIgnorePermissions && player.hasPermission("simplyvanish.flags.bypass." + material.name())) {
            return true;
        } else {
            return settings.bypassBlocks.contains(material);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onInteract(PlayerInteractEvent event) {
        // This is on highest to allow the use of info and teleport tools.
        if (event.useInteractedBlock() == PlayerInteractEvent.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        if (shouldCancel(event.getPlayer())) {
            Block block = event.getClickedBlock();
            if (block != null && hasBypass(player, block.getType())) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    // This is on highest to allow the use of info and teleport tools.
    void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (cfg.vanished.state && entity instanceof Player) {
            if (core.hasPermission(player, "simplyvanish.inventories.peek.at-all")) {
                InventoryUtil.showInventory(player, cfg, ((Player) entity), core.getSettings());
            }
        }
        if (shouldCancel(player)) {
            if (hasBypass(player, entity.getType())) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onBucketFill(PlayerBucketFillEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
            Utils.sendBlock(event.getPlayer(), event.getBlockClicked());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onBlockBreak(BlockBreakEvent event) {
        // TODO: add these for bypasses.
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // TODO: some of the following might be obsolete (interact-entity)

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setCancelled(true);
            Utils.sendBlock(event.getPlayer(), event.getBlockClicked());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onVehicleDestroy(VehicleDestroyEvent event) {
        Entity entity = event.getAttacker();
        if (entity instanceof Projectile) {
            entity = Utils.getShooterEntity((Projectile) entity);
        }
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancel((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onVehicleEnter(VehicleEnterEvent event) {
        // this could be omitted, probably
        Entity entity = event.getEntered();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancel((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onVehicleExit(VehicleExitEvent event) {
        Entity entity = event.getExited();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancel((Player) entity)) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onPaintingBreak(HangingBreakByEntityEvent event) {
        Entity entity = event.getRemover();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancel((Player) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    void onEgg(PlayerEggThrowEvent event) {
        if (shouldCancel(event.getPlayer())) {
            event.setHatching(false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onCollision(VehicleEntityCollisionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        if (shouldCancel((Player) entity)) {
            event.setPickupCancelled(true);
            event.setCollisionCancelled(true);
            // maybe that is enough:
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onInventoryOpen(InventoryOpenEvent event) {
        LivingEntity entity = event.getPlayer();
        if (!(entity instanceof Player player)) {
            return;
        }
        VanishConfig cfg = core.getVanishConfig(player, false);
        if (cfg == null) {
            return;
        }
        if (!cfg.vanished.state) {
            cfg.preventInventoryAction = false;
            return;
        }
        // TODO: extra inventory settings or permission or one more flag ?
        InventoryUtil.prepareInventoryOpen(player, event.getInventory(), cfg);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onInventoryClose(InventoryCloseEvent event) {
        LivingEntity entity = event.getPlayer();
        if (!(entity instanceof Player)) {
            return;
        }
        VanishConfig cfg = core.getVanishConfig((Player) entity, false);
        if (cfg == null) {
            return;
        }
        cfg.preventInventoryAction = false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onInventoryClick(InventoryClickEvent event) {
        LivingEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player)) {
            return;
        }
        VanishConfig cfg = core.getVanishConfig((Player) entity, false);
        if (cfg == null) {
            return;
        }
        if (cfg.preventInventoryAction) {
            event.setResult(Result.DENY);
            event.setCancelled(true);
        }
    }

}
