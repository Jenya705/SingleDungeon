package com.github.jenya705.sd.command;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class StatsCommand implements CommandExecutor {

    private final SingleDungeon plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            plugin.getStatsGUI().openGui(player);
        }
        else {
            sender.sendMessage("Only for players!");
        }
        return true;
    }
}
