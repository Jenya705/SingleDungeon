package com.github.jenya705.sd;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.github.jenya705.sd.arena.ArenaListener;
import com.github.jenya705.sd.arena.ArenaManager;
import com.github.jenya705.sd.command.LeaveCommand;
import com.github.jenya705.sd.command.StartCommand;
import com.github.jenya705.sd.command.StatsCommand;
import com.github.jenya705.sd.config.SDConfig;
import com.github.jenya705.sd.mob.MobHider;
import com.github.jenya705.sd.stats.StatsContainer;
import com.github.jenya705.sd.stats.StatsGUI;
import com.github.jenya705.sd.stats.StatsUpdateTask;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class SingleDungeon extends JavaPlugin {

    private final SDConfig sdConfig = new SDConfig(this);
    private final ArenaManager arenaManager = new ArenaManager(this);

    private StatsContainer statsContainer;
    private MobHider mobHider;
    private ProtocolManager protocolManager;
    private StatsGUI statsGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        protocolManager = ProtocolLibrary.getProtocolManager();
        registerListeners(
                mobHider = new MobHider(this),
                statsContainer = new StatsContainer(this),
                statsGUI = new StatsGUI(this),
                new ArenaListener(this)
        );
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
    }

    @Override
    public void onDisable() {
        statsContainer.getUpdateTask().update(StatsUpdateTask.ALL);
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

}
