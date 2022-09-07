package com.github.jenya705.sd.stats;

import com.github.jenya705.sd.SingleDungeon;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class StatsContainer implements Listener {

    final Deque<PlayerStats> updateDeque = new ConcurrentLinkedDeque<>();

    @Getter
    private final HikariDataSource dataSource;

    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final SingleDungeon plugin;

    // System from leavers who will try to load themselves many times. It will delay removing their stats from cache
    private final Map<UUID, BukkitTask> removeTasks = new HashMap<>();

    @Getter
    private final StatsUpdateTask updateTask;

    public StatsContainer(SingleDungeon plugin) {
        this.plugin = plugin;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://%s/%s".formatted(
                    plugin.getConfig().getString("mysql.host"),
                    plugin.getConfig().getString("mysql.database")
            ));
            config.setUsername(plugin.getConfig().getString("mysql.user"));
            config.setPassword(plugin.getConfig().getString("mysql.password"));
            dataSource = new HikariDataSource(config);
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS player_stats (
                            uuid VARCHAR(36) PRIMARY KEY,
                            mob_kills INT NOT NULL,
                            sessions INT NOT NULL,
                            deaths INT NOT NULL
                        );
                        """);
            }
            updateTask = new StatsUpdateTask(plugin);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, updateTask);
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    public PlayerStats read(UUID uuid) {
        return get(uuid);
    }

    public void update(UUID uuid, Consumer<PlayerStats> consumer) {
        PlayerStats stats = get(uuid);
        consumer.accept(stats);
        updateDeque.addLast(stats);
    }

    private PlayerStats get(UUID uuid) {
        PlayerStats reference = playerStats.get(uuid);
        if (reference == null) throw new IllegalStateException("Updated player is not online");
        return reference;
    }

    @SneakyThrows
    private PlayerStats fetch(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT mob_kills, sessions, deaths FROM player_stats WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            PlayerStats result = new PlayerStats(uuid);
            if (resultSet.next()) {
                result.setMobKills(resultSet.getInt(1));
                result.setSessions(resultSet.getInt(2));
                result.setDeaths(resultSet.getInt(3));
            }
            resultSet.close();
            return result;
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        BukkitTask removeTask = removeTasks.get(event.getPlayer().getUniqueId());
        if (removeTask != null) removeTask.cancel();
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                playerStats.computeIfAbsent(uuid, this::fetch)
        );
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        removeTasks.put(
                event.getPlayer().getUniqueId(),
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> playerStats.remove(event.getPlayer().getUniqueId()),
                        1000 // 1000 / 20 = 50 seconds
                )
        );
    }

}
