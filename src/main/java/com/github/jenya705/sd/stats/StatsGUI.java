package com.github.jenya705.sd.stats;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class StatsGUI implements Listener {

    private final Set<UUID> inGui = new HashSet<>();
    private final SingleDungeon plugin;

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(inGui.contains(player.getUniqueId()))) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        inGui.remove(event.getPlayer().getUniqueId());
    }

    public void openGui(Player player) {
        PlayerStats stats = plugin.getStatsContainer().read(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 9);
        inventory.setItem(
                1,
                createItem(Material.ZOMBIE_HEAD, ChatColor.RED + "Mob kills: " + stats.getMobKills())
        );
        inventory.setItem(
                3,
                createItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Sessions: " + stats.getSessions())
        );
        inventory.setItem(
                5,
                createItem(Material.REDSTONE, ChatColor.DARK_RED + "Average kills per session: " +
                        "%.3f".formatted(stats.getAverageKills())
                )
        );
        inventory.setItem(
                7,
                createItem(Material.SKELETON_SKULL, ChatColor.GRAY + "Deaths: " + stats.getDeaths())
        );
        inGui.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String itemName) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(itemName);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

}
