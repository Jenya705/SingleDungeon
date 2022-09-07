package com.github.jenya705.sd.arena;

import com.github.jenya705.sd.SingleDungeon;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ArenaManager {

    @Data
    public static class Session {
        private final List<Mob> mobs = new ArrayList<>();
        private final ArenaManager arenaManager;
        private final PlayerData playerData;
        private final Player player;

        private BukkitTask waveSpawnTask;

        private int currentWave = 1;

        public void addMob(Mob mob) {
            arenaManager.mobs.put(mob.getUniqueId(), new SessionLinkedMob(mob, this));
            arenaManager.mobsId.add(mob.getEntityId());
            mobs.add(mob);
        }
    }

    @RequiredArgsConstructor
    public static class PlayerData {

        public static PlayerData fromPlayer(Player player) {
            return new PlayerData(
                    Arrays.copyOf(player.getInventory().getContents(), player.getInventory().getSize()),
                    player.getFoodLevel(),
                    player.getExhaustion(),
                    player.getHealth(),
                    player.getRemainingAir(),
                    player.getTotalExperience(),
                    new ArrayList<>(player.getActivePotionEffects()),
                    player.getLocation()
            );
        }

        private final ItemStack[] inventory;
        private final int food;
        private final float exhaustion;
        private final double health;
        private final int air;
        private final int exp;
        private final List<PotionEffect> effects;
        private final Location location;

        public void rollback(Player player) {
            player.getInventory().setContents(inventory);
            player.setFoodLevel(food);
            player.setExhaustion(exhaustion);
            player.setHealth(health);
            player.setRemainingAir(air);
            player.setTotalExperience(exp);
            player.getActivePotionEffects()
                    .forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            effects.forEach(player::addPotionEffect);
            player.teleport(location);
        }


    }

    @Data
    private static class SessionLinkedMob {
        private final Mob mob;
        private final Session session;
    }

    public static void fillPlayerInventory(PlayerInventory playerInventory) {
        playerInventory.clear();
        playerInventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        playerInventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        playerInventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        playerInventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        playerInventory.setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        playerInventory.setItem(1, new ItemStack(Material.GOLDEN_APPLE, 32));
    }

    public static void setPlayerDefaults(Player player) {
        fillPlayerInventory(player.getInventory());
        player.setRemainingAir(player.getMaximumAir());
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        player.setFoodLevel(20);
        player.setExhaustion(20);
        player.setTotalExperience(0);
        player.getActivePotionEffects()
                .forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
    }

    private final SingleDungeon plugin;

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, SessionLinkedMob> mobs = new HashMap<>();
    private final Set<Integer> mobsId = new HashSet<>();

    public void enter(Player player) {
        Session arenaSession = new Session(
                this,
                PlayerData.fromPlayer(player),
                player
        );
        setPlayerDefaults(player);
        player.teleport(plugin.getSdConfig().getDungeonSpawn());
        mobsId.add(player.getEntityId());
        plugin.getMobHider().hideEntities(player, Collections.singletonList(player));
        sessions.put(player.getUniqueId(), arenaSession);
        arenaSession.waveSpawnTask =
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> {
                            spawnMobs(arenaSession, player);
                            plugin.getStatsContainer().update(
                                    player.getUniqueId(),
                                    stats -> stats.setSessions(stats.getSessions() + 1)
                            );
                        },
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
            plugin.getMobHider().hideEntities(player, session.mobs);
        });
        setCollisions(player, session.mobs);
        player.sendMessage(session.currentWave + " wave spawned");
    }

    public Session getSession(UUID player) {
        return sessions.get(player);
    }

    public boolean removeMob(Mob mob) {
        SessionLinkedMob linkedMob = mobs.remove(mob.getUniqueId());
        if (linkedMob == null) return false;
        linkedMob.session.waveSpawnTask =
                Bukkit.getScheduler().runTask(plugin, () -> {
                    linkedMob.session.mobs.remove(mob);
                    mobsId.remove(mob.getEntityId());
                    if (linkedMob.session.mobs.isEmpty()) {
                        nextWave(linkedMob.session);
                    }
                });
        plugin.getStatsContainer().update(
                linkedMob.session.player.getUniqueId(),
                stats -> stats.setMobKills(stats.getMobKills() + 1)
        );
        return true;
    }

    public boolean endSession(Player player, boolean death) {
        Session arenaSession = sessions.remove(player.getUniqueId());
        if (arenaSession == null) return false;
        if (arenaSession.waveSpawnTask != null) {
            arenaSession.waveSpawnTask.cancel();
        }
        arenaSession.getMobs().forEach(mob -> {
            mob.remove();
            mobsId.remove(mob.getEntityId());
            mobs.remove(mob.getUniqueId());
        });
        mobsId.remove(player.getEntityId());
        arenaSession.playerData.rollback(player);
        removePlayerCollisions(player);
        plugin.getProtocolManager().updateEntity(
                player,
                player.getWorld().getNearbyEntities(
                        player.getLocation(),
                        Bukkit.getViewDistance() * 16,
                        Bukkit.getViewDistance() * 16,
                        Bukkit.getViewDistance() * 16,
                        e -> e instanceof Player
                ).stream().map(e -> (Player) e).toList()
        );
        if (death) {
            plugin.getStatsContainer().update(
                    player.getUniqueId(),
                    stats -> stats.setDeaths(stats.getDeaths() + 1)
            );
        }
        return true;
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

    public boolean isAvailableMob(Player player, Mob mob) {
        if (player.equals(mob)) return true;
        Session session = getSession(player.getUniqueId());
        if (session == null) return true;
        SessionLinkedMob linkedMob = mobs.get(mob.getUniqueId());
        if (linkedMob == null) return false;
        return linkedMob.session.player.equals(player);
    }

    private static void setCollisions(Player player, List<Mob> mobs) {
        List<UUID> collisions = mobs.stream()
                .map(Mob::getUniqueId)
                .collect(Collectors.toList());
        player.setCollidable(false);
        player.getCollidableExemptions().clear();
        player.getCollidableExemptions().addAll(collisions);
        collisions.add(player.getUniqueId());
        mobs.forEach(mob -> {
            mob.setCollidable(false);
            mob.getCollidableExemptions().addAll(collisions);
        });
    }

    private static void removePlayerCollisions(Player player) {
        player.setCollidable(true);
        player.getCollidableExemptions().clear();
    }

}
