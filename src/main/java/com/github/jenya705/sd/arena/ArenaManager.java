package com.github.jenya705.sd.arena;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.github.jenya705.sd.SingleDungeon;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ArenaManager {

    @Data
    public static class Session {
        private final List<Mob> mobs = new ArrayList<>();
        private final ArenaManager arenaManager;
        private final Location previousPlayerLocation;
        private final Player player;

        private int currentWave = 1;

        public void addMob(Mob mob) {
            arenaManager.mobs.put(mob.getUniqueId(), new SessionLinkedMob(mob, this));
            arenaManager.mobsId.add(mob.getEntityId());
            mobs.add(mob);
        }
    }

    @Data
    private static class SessionLinkedMob {
        private final Mob mob;
        private final Session session;
    }

    private final SingleDungeon plugin;

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, SessionLinkedMob> mobs = new HashMap<>();
    private final Set<Integer> mobsId = new HashSet<>();

    public void enter(Player player) {
        Location currentPlayerLocation = player.getLocation();
        player.teleport(plugin.getSdConfig().getDungeonSpawn());
        Session arenaSession = new Session(
                this,
                currentPlayerLocation,
                player
        );
        sessions.put(player.getUniqueId(), arenaSession);
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> spawnMobs(arenaSession, player),
                100
        );
    }

    private final static EntityType[] MOB_SPAWNS = new EntityType[]{
            EntityType.ZOMBIE
    };

    private void spawnMobs(Session session, Player player) {
        Location dungeonSpawn = plugin.getSdConfig().getDungeonSpawn();
        plugin.getMobHider().doSpawning(player.getUniqueId(), () -> {
            for (EntityType entityType : MOB_SPAWNS) {
                for (int i = 0; i < session.currentWave; ++i) {
                    Mob spawned = (Mob) dungeonSpawn.getWorld().spawnEntity(
                            dungeonSpawn,
                            entityType,
                            false
                    );
                    session.addMob(spawned);
                    spawned.setTarget(player);
                }
            }
            PacketContainer removePacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            removePacket.getIntLists().write(
                    0,
                    session.mobs.stream().map(Mob::getEntityId).toList()
            );
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player)) continue;
                // TODO. Try to find better way
                try {
                    plugin.getProtocolManager().sendServerPacket(onlinePlayer, removePacket);
                } catch (InvocationTargetException e) {
                    plugin.getLogger().log(Level.SEVERE, "Exception while sending ENTITY_DESTROY packet:", e);
                }
            }
        });
    }

    public Session getSession(UUID player) {
        return sessions.get(player);
    }

    public void removeMob(Mob mob) {
        SessionLinkedMob linkedMob = mobs.remove(mob.getUniqueId());
        if (linkedMob == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            linkedMob.session.mobs.remove(mob);
            mobsId.remove(mob.getEntityId());
            if (linkedMob.session.mobs.isEmpty()) {
                nextWave(linkedMob.session);
            }
        });
    }

    public void endSession(Player player) {
        Session arenaSession = sessions.remove(player.getUniqueId());
        if (arenaSession == null) return;
        arenaSession.getMobs().forEach(mob -> {
            mob.remove();
            mobsId.remove(mob.getEntityId());
            mobs.remove(mob.getUniqueId());
        });
        player.teleport(arenaSession.getPreviousPlayerLocation());
    }

    public void nextWave(Session session) {
        session.currentWave++;
        spawnMobs(session, session.player);
    }

    public void targetMob(Mob mob, Entity target, Consumer<Entity> targetSetter) {
        SessionLinkedMob linkedMob = mobs.get(mob.getUniqueId());
        if (linkedMob == null) return;
        if (!(target instanceof Player player) || !player.equals(linkedMob.session.player)) {
            targetSetter.accept(linkedMob.session.player);
        }
    }

    public boolean isArenaMob(int mobId) {
        return mobsId.contains(mobId);
    }

}
