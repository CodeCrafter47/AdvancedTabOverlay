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
import de.codecrafter47.data.bukkit.AbstractBukkitDataAccess;
import de.codecrafter47.data.bukkit.PlayerDataAccess;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.config.misc.Unchecked;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DataManager {

    private final AdvancedTabOverlay plugin;
    private final Set<PlayerDataHolder> dataHolderSet = Sets.newConcurrentHashSet();

    private final List<AbstractBukkitDataAccess<?>> dataAccesses = new CopyOnWriteArrayList<>();
    private DataAccess<Player> playerDataAccess;

    public DataManager(AdvancedTabOverlay plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        updateHooks();
        plugin.getAsyncExecutor().scheduleWithFixedDelay(this::updateDataAsync, 1, 1, TimeUnit.SECONDS);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::updateDataSync, 20, 20);
    }

    public void updateHooks() {
        dataAccesses.clear();
        boolean hasPlaceholderAPI = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        PlayerDataAccess playerDataAccess1 = new PlayerDataAccess(plugin);
        dataAccesses.add(playerDataAccess1);
        if (hasPlaceholderAPI) {
            PAPIDataAccess papiDataAccess = new PAPIDataAccess(plugin.getLogger(), plugin);
            dataAccesses.add(papiDataAccess);
            playerDataAccess = JoinedDataAccess.of(playerDataAccess1, new ATODataAccess(plugin.getLogger(), plugin), papiDataAccess);
        } else {
            playerDataAccess = JoinedDataAccess.of(playerDataAccess1, new ATODataAccess(plugin.getLogger(), plugin));
        }
    }

    private void updateDataAsync() {
        try {
            for (PlayerDataHolder dataHolder : dataHolderSet) {
                dataHolder.update(plugin.getTabEventQueue(), playerDataAccess, false);
            }
        } catch (Throwable th) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected exception", th);
        }
    }

    private void updateDataSync() {
        try {
            for (PlayerDataHolder dataHolder : dataHolderSet) {
                dataHolder.update(plugin.getTabEventQueue(), playerDataAccess, true);
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

        private final Set<DataKey<?>> activeKeys = Sets.newConcurrentHashSet();

        private final Player player;

        private PlayerDataHolder(Player player) {
            this.player = player;
        }

        private boolean requiresMainThread(@Nonnull DataKey<?> key) {
            for (AbstractBukkitDataAccess<?> dataAccess : DataManager.this.dataAccesses) {
                if (dataAccess.requiresMainThread(key)) {
                    return true;
                }
            }
            return false;
        }

        void update(ScheduledExecutorService tabEventQueue, DataAccess<Player> playerDataAccess, boolean isMainThread) {
            for (DataKey<?> activeKey : activeKeys) {
                DataKey<Object> dataKey = Unchecked.cast(activeKey);
                if (requiresMainThread(dataKey) != isMainThread) {
                    continue;
                }
                Object o = null;
                try {
                    o = playerDataAccess.get(dataKey, player);
                } catch (Throwable e) {
                    plugin.getLogger().log(Level.SEVERE, "Unexpected exception resolving datakey " + dataKey.getId(), e);
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
