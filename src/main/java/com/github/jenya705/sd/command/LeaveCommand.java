package com.github.jenya705.sd.command;

import com.github.jenya705.sd.SingleDungeon;
import com.github.jenya705.sd.arena.ArenaManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class LeaveCommand implements CommandExecutor {

    private final SingleDungeon plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            ArenaManager.Session session = plugin.getArenaManager().getSession(player.getUniqueId());
            if (session == null) {
                sender.sendMessage("You are not on the arena");
            }
            else {
                plugin.getArenaManager().endSession(player);
            }
        }
        else {
            sender.sendMessage("For players!");
        }
        return true;
    }
}
