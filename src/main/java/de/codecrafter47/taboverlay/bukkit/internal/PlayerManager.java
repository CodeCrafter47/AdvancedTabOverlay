/*
 *     Copyright (C) 2020 Florian Stober
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
