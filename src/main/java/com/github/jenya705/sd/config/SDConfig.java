package com.github.jenya705.sd.config;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

@RequiredArgsConstructor
public class SDConfig {

    private final SingleDungeon plugin;

    public Location getDungeonSpawn() {
        return new Location(
                plugin.getServer().getWorld(
                        plugin.getConfig().getString("dungeon-spawn.world", "world")),
                plugin.getConfig().getInt("dungeon-spawn.x"),
                plugin.getConfig().getInt("dungeon-spawn.y"),
                plugin.getConfig().getInt("dungeon-spawn.z")
        );
    }

}
