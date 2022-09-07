package com.github.jenya705.sd.stats;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class StatsGUI implements Listener {

    private final Set<UUID> inGui = new HashSet<>();
    private final SingleDungeon plugin;

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory().equals(event.getInventory()))) return;
        if (!(inGui.contains(player.getUniqueId()))) return;
        event.setCancelled(true);
    }

    public void openGui(Player player) {
        PlayerStats stats = plugin.getStatsContainer().read(player.getUniqueId());
        player.sendMessage(
                """
                        Mob kills: %s
                        Sessions: %s
                        Average kills: %.3f
                        Deaths: %s
                        """.formatted(
                        Integer.toString(stats.getMobKills()),
                        Integer.toString(stats.getSessions()),
                        stats.getAverageKills(),
                        Integer.toString(stats.getDeaths())
                )
        );
    }

    private ItemStack createWithLore(Material material, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

}
