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

import com.google.common.collect.Sets;
import de.codecrafter47.data.api.DataAccess;
import de.codecrafter47.data.api.DataCache;
import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.api.JoinedDataAccess;
import de.codecrafter47.data.bukkit.PlayerDataAccess;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.config.misc.Unchecked;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DataManager {

    private final AdvancedTabOverlay plugin;
    private final Set<PlayerDataHolder> dataHolderSet = Sets.newConcurrentHashSet();

    private DataAccess<Player> playerDataAccess;

    public DataManager(AdvancedTabOverlay plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        boolean hasPlaceholderAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (hasPlaceholderAPI) {
            playerDataAccess = JoinedDataAccess.of(new PlayerDataAccess(plugin), new ATODataAccess(plugin.getLogger(), plugin), new PAPIDataAccess(plugin.getLogger(), plugin));
        } else {
            playerDataAccess = JoinedDataAccess.of(new PlayerDataAccess(plugin), new ATODataAccess(plugin.getLogger(), plugin));
        }
        plugin.getAsyncExecutor().scheduleWithFixedDelay(this::updateData, 1, 1, TimeUnit.SECONDS);
    }

    public void updateHooks() {
        boolean hasPlaceholderAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (hasPlaceholderAPI) {
            playerDataAccess = JoinedDataAccess.of(new PlayerDataAccess(plugin), new ATODataAccess(plugin.getLogger(), plugin), new PAPIDataAccess(plugin.getLogger(), plugin));
        } else {
            playerDataAccess = JoinedDataAccess.of(new PlayerDataAccess(plugin), new ATODataAccess(plugin.getLogger(), plugin));
        }
    }

    private void updateData() {
        try {
            for (PlayerDataHolder dataHolder : dataHolderSet) {
                dataHolder.update(plugin.getTabEventQueue(), playerDataAccess);
            }
        } catch (Throwable th) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected exception", th);
        }
    }

    PlayerDataHolder createDataHolder(Player player) {
        PlayerDataHolder dataHolder = new PlayerDataHolder(player);
        dataHolderSet.add(dataHolder);
        return dataHolder;
    }

    void removeDataHolder(PlayerDataHolder dataHolder) {
        dataHolderSet.remove(dataHolder);
    }

    class PlayerDataHolder extends DataCache {

        private Set<DataKey<?>> activeKeys = Sets.newConcurrentHashSet();

        private final Player player;

        private PlayerDataHolder(Player player) {
            this.player = player;
        }

        void update(ScheduledExecutorService tabEventQueue, DataAccess<Player> playerDataAccess) {
            for (DataKey<?> activeKey : activeKeys) {
                DataKey<Object> dataKey = Unchecked.cast(activeKey);
                Object o = null;
                try {
                    o = playerDataAccess.get(dataKey, player);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (!Objects.equals(o, get(dataKey))) {
                    Object finalO = o;
                    tabEventQueue.submit(() -> {
                        try {
                            super.updateValue(dataKey, finalO);
                        } catch (Throwable th) {
                            plugin.getLogger().log(Level.SEVERE, "Unexpected exception.", th);
                        }
                    });
                }
            }
        }

        @Override
        public <V> V get(DataKey<V> key) {
            // todo remove this safety check for performance reasons
            if (!activeKeys.contains(key) && plugin.getTabEventQueue().inEventLoop()) {
                throw new IllegalStateException("No listener registered for datakey " + key);
            }
            return super.get(key);
        }

        @Override
        public <T> void addDataChangeListener(DataKey<T> key, Runnable listener) {
            super.addDataChangeListener(key, listener);
            activeKeys.add(key);
        }

        @Override
        public <T> void removeDataChangeListener(DataKey<T> key, Runnable listener) {
            super.removeDataChangeListener(key, listener);
            if (!hasListeners(key)) {
                activeKeys.remove(key);
            }
        }
    }
}
