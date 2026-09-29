package com.mdevstudio.rulesgate.gate;

import io.papermc.paper.persistence.PersistentDataContainerView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Keeps acceptances in each player's own data, so there is no database to set up or back up separately.
 */
public final class AcceptanceStore {

    private final NamespacedKey versionKey;
    private final NamespacedKey acceptedAtKey;
    // Rules are accepted before the player entity exists; the answer waits here until the player joins.
    private final Map<UUID, Acceptance> waitingForJoin = new ConcurrentHashMap<>();

    public AcceptanceStore(Plugin plugin) {
        this.versionKey = new NamespacedKey(plugin, "accepted_version");
        this.acceptedAtKey = new NamespacedKey(plugin, "accepted_at");
    }

    public Acceptance read(OfflinePlayer player) {
        Acceptance waiting = waitingForJoin.get(player.getUniqueId());
        if (waiting != null) {
            return waiting;
        }
        PersistentDataContainerView data = player.getPersistentDataContainer();
        Integer version = data.get(versionKey, PersistentDataType.INTEGER);
        Long acceptedAt = data.get(acceptedAtKey, PersistentDataType.LONG);
        return version != null && acceptedAt != null
                ? new Acceptance(version, Instant.ofEpochMilli(acceptedAt))
                : null;
    }

    public void acceptBeforeJoin(UUID player, Acceptance acceptance) {
        waitingForJoin.put(player, acceptance);
    }

    public void accept(Player player, Acceptance acceptance) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(versionKey, PersistentDataType.INTEGER, acceptance.version());
        data.set(acceptedAtKey, PersistentDataType.LONG, acceptance.acceptedAt().toEpochMilli());
    }

    public void saveWaiting(Player player) {
        Acceptance acceptance = waitingForJoin.remove(player.getUniqueId());
        if (acceptance != null) {
            accept(player, acceptance);
        }
    }

    public void forget(UUID player) {
        waitingForJoin.remove(player);
    }
}
