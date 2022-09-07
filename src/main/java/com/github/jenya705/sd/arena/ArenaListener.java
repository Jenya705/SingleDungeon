package com.github.jenya705.sd.arena;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class ArenaListener implements Listener {

    private final SingleDungeon plugin;

    @EventHandler
    public void entityDeath(EntityDeathEvent event) {
        boolean clearDrops;
        if (event.getEntity() instanceof Player player) {
            clearDrops = plugin.getArenaManager().endSession(player, true);
        }
        else if (event.getEntity() instanceof Mob mob) {
            clearDrops = plugin.getArenaManager().removeMob(mob);
        }
        else return;
        if (clearDrops) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        // TODO is it death?
        plugin.getArenaManager().endSession(event.getPlayer(), false);
    }

    @EventHandler
    public void target(EntityTargetEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            plugin.getArenaManager().targetMob(mob, event.getTarget(), event::setTarget);
        }
    }

    @EventHandler
    public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) ||
            !(event.getEntity() instanceof Mob mob)) return;
        if (!plugin.getArenaManager().isAvailableMob(player, mob)) {
            event.setCancelled(true);
        }
    }

}
