package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.config.player.Player;
import de.codecrafter47.taboverlay.config.player.PlayerProvider;
import lombok.NonNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;

public class PlayerManager implements PlayerProvider {

    private final AdvancedTabOverlay plugin;
    private final Map<UUID, BukkitPlayer> playerMap = new HashMap<>();
    private final Set<Listener> listenerSet = new HashSet<>();

    public PlayerManager(AdvancedTabOverlay plugin) {
        this.plugin = plugin;
    }

    public Player onPlayerJoin(org.bukkit.entity.Player bukkitPlayer) {
        BukkitPlayer player = new BukkitPlayer(bukkitPlayer);
        plugin.getTabEventQueue().execute(() -> {
            playerMap.put(bukkitPlayer.getUniqueId(), player);
            for (Listener listener : listenerSet) {
                try {
                    listener.onPlayerAdded(player);
                } catch (Throwable th) {
                    plugin.getLogger().log(Level.SEVERE, "Error invoking player login listener", th);
                }
            }
        });
        return player;
    }

    public void onPlayerDisconnect(org.bukkit.entity.Player bukkitPlayer) {
        plugin.getTabEventQueue().execute(() -> {
            BukkitPlayer player = playerMap.remove(bukkitPlayer.getUniqueId());
            if (player != null) {
                for (Listener listener : listenerSet) {
                    try {
                        listener.onPlayerRemoved(player);
                    } catch (Throwable th) {
                        plugin.getLogger().log(Level.SEVERE, "Error invoking player disconnect listener", th);
                    }
                }
                player.onDisconnect();
            } else {
                throw new IllegalStateException("Player " + bukkitPlayer + " not contained in playerMap");
            }
        });
    }

    @Override
    public Collection<? extends Player> getPlayers() {
        return playerMap.values();
    }

    @Override
    public void registerListener(@Nonnull @NonNull Listener listener) {
        listenerSet.add(listener);
    }

    @Override
    public void unregisterListener(@Nonnull @NonNull Listener listener) {
        listenerSet.remove(listener);
    }

    @Nonnull
    @NonNull
    public Player getPlayer(@Nonnull @NonNull org.bukkit.entity.Player player) {
        return Objects.requireNonNull(playerMap.get(player.getUniqueId()));
    }
}
