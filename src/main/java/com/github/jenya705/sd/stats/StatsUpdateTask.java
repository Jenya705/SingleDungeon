package com.github.jenya705.sd.stats;

import com.github.jenya705.sd.SingleDungeon;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

@RequiredArgsConstructor
public class StatsUpdateTask implements Runnable {

    public static final int ALL = -1;

    private static final int BATCH_SIZE = 30;

    private final SingleDungeon plugin;

    public void update(int maxCount) {
        Deque<PlayerStats> deque = plugin.getStatsContainer().updateDeque;
        if (deque.isEmpty()) return;
        List<PlayerStats> toReturn = new ArrayList<>();
        Set<UUID> updated = new HashSet<>();
        try (Connection connection = plugin.getStatsContainer().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO player_stats (uuid, mob_kills, sessions, deaths)
                     VALUES (?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                     mob_kills = ?, sessions = ?, deaths = ?
                     """)) {
            int counter = 0;
            while (counter != maxCount && deque.peek() != null) {
                PlayerStats stats = deque.poll();
                if (!updated.add(stats.getUuid())) {
                    continue;
                }
                toReturn.add(stats);
                statement.setString(1, stats.getUuid().toString());
                statement.setInt(2, stats.getMobKills());
                statement.setInt(3, stats.getSessions());
                statement.setInt(4, stats.getDeaths());
                statement.setInt(5, stats.getMobKills());
                statement.setInt(6, stats.getSessions());
                statement.setInt(7, stats.getDeaths());
                statement.addBatch();
                counter++;
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update player_stats, undo changes in deque:", e);
            toReturn.forEach(deque::addFirst);
        }
    }

    @Override
    public void run() {
        update(BATCH_SIZE);
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, 5);
        }
    }

}
