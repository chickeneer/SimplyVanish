package dev.chickeneer.simplyvanish.inventories;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.config.Settings;
import dev.chickeneer.simplyvanish.config.VanishConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InventoryUtil {

    /**
     * Show inventory based on settings.
     *
     * @param viewer
     * @param settings
     */
    public static void showInventory(@NotNull CommandSender viewer, @Nullable VanishConfig cfg, @NotNull Player other, @NotNull Settings settings) {
        if (settings.allowRealPeek && viewer instanceof Player && SimplyVanish.hasPermission(viewer, "simplyvanish.inventories.peek.real")) {
            Player player = (Player) viewer;
            Bukkit.getScheduler().scheduleSyncDelayedTask(SimplyVanish.getPluginInstance(), () -> {
                player.closeInventory();
                final Inventory inv = other.getInventory();
                prepareInventoryOpen(player, inv, cfg); // TODO
                // TODO: trigger OpenInv if modifiable !
                player.openInventory(inv);
            });
        } else {
            List<ItemStack> items = new LinkedList<>(Arrays.asList(other.getInventory().getContents()));
            StringBuilder b = new StringBuilder();
            b.append("Inventory(").append(other.getName()).append("): ");
            addItemDescription(items, b);
            viewer.sendMessage(b.toString());
        }
    }

    /**
     * Set the preventInventoryAction flag according to permissions.
     *
     * @param player
     * @param inventory
     * @param cfg
     */
    public static void prepareInventoryOpen(@NotNull Player player, @NotNull Inventory inventory, @Nullable VanishConfig cfg) {
        if (cfg != null) {
            if (SimplyVanish.hasPermission(player, "simplyvanish.inventories.manipulate")) {
                cfg.preventInventoryAction = false;
            } else {
                cfg.preventInventoryAction = inventory != player.getInventory();
            }
        }
    }

    /**
     * Add verbalized and sorted item descriptions.
     * @param items
     * @param builder
     */
	public static void addItemDescription(@NotNull Collection<ItemStack> items, @NotNull StringBuilder builder) {
		if (items.isEmpty()) return;
		List<String> keys = new ArrayList<>(items.size()); // will rather be shorter.
		Map<String, Integer>  dropList = new HashMap<>();
		for ( ItemStack stack:items){
			if (stack == null || stack.getType() == Material.AIR) {
			    continue;
            }
			StringBuilder key = new StringBuilder(stack.getType().toString().toLowerCase());
			Map<Enchantment, Integer> enchantments = stack.getEnchantments();
            if (!enchantments.isEmpty()){
                List<String> es = new ArrayList<>(enchantments.size());
                for (Enchantment e : enchantments.keySet()){
                    es.add(e.getName()+"@"+enchantments.get(e));
                }
                Collections.sort(es);
                key.append("(");
                for (String s : es){
                    key.append(s).append(",");
                }
                key.append(")");
            }
            Integer n = dropList.get(key.toString());
			if ( n != null) dropList.put(key.toString(), n + stack.getAmount());
			else{
				dropList.put(key.toString(),  stack.getAmount());
				keys.add(key.toString());
			}
		}
		Collections.sort(keys);
		for ( String key : keys){
			builder.append(key).append(" x").append(dropList.get(key)).append(", ");
		}
	}

}
