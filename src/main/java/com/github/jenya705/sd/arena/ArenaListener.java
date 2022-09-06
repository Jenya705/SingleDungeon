package com.github.jenya705.sd.arena;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class ArenaListener implements Listener {

    private final SingleDungeon plugin;

    @EventHandler
    public void entityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getArenaManager().endSession(player);
        }
        else if (event.getEntity() instanceof Mob mob) {
            plugin.getArenaManager().removeMob(mob);
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        plugin.getArenaManager().endSession(event.getPlayer());
    }

    @EventHandler
    public void target(EntityTargetEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            plugin.getArenaManager().targetMob(mob, event.getTarget(), event::setTarget);
        }
    }

}
