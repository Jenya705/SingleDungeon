package com.github.jenya705.sd.mob;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.jenya705.sd.SingleDungeon;
import com.github.jenya705.sd.arena.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MobHider extends PacketAdapter implements Listener {

    private static final PacketType[] ENTITY_PACKETS = {
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ANIMATION,
            PacketType.Play.Server.NAMED_ENTITY_SPAWN,
            PacketType.Play.Server.COLLECT,
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.SPAWN_ENTITY_LIVING,
            PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
            PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_LOOK,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_HEAD_ROTATION,
            PacketType.Play.Server.ENTITY_STATUS,
            PacketType.Play.Server.ATTACH_ENTITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_EFFECT,
            PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
            PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
            PacketType.Play.Server.PLAYER_COMBAT_KILL
    };

    private final SingleDungeon plugin;

    private UUID currentSpawning;

    public void doSpawning(UUID uuid, Runnable runnable) {
        validateBukkitThread();
        currentSpawning = uuid;
        runnable.run();
        currentSpawning = null;
    }

    private void validateBukkitThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Should be executed on the main thread!");
    }

    public MobHider(SingleDungeon plugin) {
        super(plugin, ENTITY_PACKETS);
        this.plugin = plugin;
        plugin.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        int index = event.getPacketType() == PacketType.Play.Server.PLAYER_COMBAT_KILL ? 1 : 0;
        Integer entityID = event.getPacket().getIntegers().readSafely(index);
        if (entityID != null &&
//                !event.getPlayer().getUniqueId().equals(currentSpawning) &&
                !isVisible(event.getPlayer(), entityID)) {
            event.setCancelled(true);
        }
    }

    private boolean isVisible(Player player, int entityID) {
        // Player always visible to himself
        if (player.getEntityId() == entityID) return true;
        ArenaManager.Session arenaSession = plugin.getArenaManager()
                .getSession(player.getUniqueId());
        boolean arenaMob = plugin.getArenaManager().isArenaMob(entityID);
        if (arenaSession == null) return !arenaMob;
        return !arenaMob || arenaSession.getMobs()
                .stream()
                .anyMatch(mob -> mob.getEntityId() == entityID);
    }

    public void hideEntities(Player except, List<? extends Entity> entities) {
        PacketContainer removePacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        removePacket.getIntLists().write(
                0,
                entities.stream().map(Entity::getEntityId).toList()
        );
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.equals(except)) continue;
            try {
                plugin.getProtocolManager().sendServerPacket(onlinePlayer, removePacket);
            } catch (InvocationTargetException e) {
                plugin.getLogger().log(Level.SEVERE, "Exception while sending ENTITY_DESTROY packet:", e);
            }
        }
    }

}
