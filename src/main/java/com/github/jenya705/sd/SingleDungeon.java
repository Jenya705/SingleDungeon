package com.github.jenya705.sd;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.github.jenya705.sd.arena.ArenaListener;
import com.github.jenya705.sd.arena.ArenaManager;
import com.github.jenya705.sd.command.LeaveCommand;
import com.github.jenya705.sd.command.StartCommand;
import com.github.jenya705.sd.config.SDConfig;
import com.github.jenya705.sd.mob.MobHider;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class SingleDungeon extends JavaPlugin {

    private final SDConfig sdConfig = new SDConfig(this);
    private final ArenaManager arenaManager = new ArenaManager(this);
    private MobHider mobHider;
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        protocolManager = ProtocolLibrary.getProtocolManager();
        registerListeners(
                mobHider = new MobHider(this),
                new ArenaListener(this)
        );
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
    }

    @Override
    public void onDisable() {

    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener: listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

}
