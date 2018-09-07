package de.codecrafter47.taboverlay.bukkit.internal;

import com.google.common.collect.Sets;
import de.codecrafter47.data.api.*;
import de.codecrafter47.data.bukkit.PlayerDataAccess;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.util.Unchecked;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
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
                    tabEventQueue.execute(() -> super.updateValue(dataKey, finalO));
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
            if (!hasListeners(key)) {
                try {
                    super.updateValue(key, playerDataAccess.get(key, player));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
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
