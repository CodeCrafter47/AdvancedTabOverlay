package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.config.player.Player;
import lombok.NonNull;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.UUID;

public class BukkitPlayer implements Player {
    private final org.bukkit.entity.Player bukkitPlayer;
    private final DataManager.PlayerDataHolder dataHolder;

    public BukkitPlayer(org.bukkit.entity.Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        this.dataHolder = JavaPlugin.getPlugin(AdvancedTabOverlay.class).getDataManager().createDataHolder(bukkitPlayer);
    }

    void onDisconnect() {
        JavaPlugin.getPlugin(AdvancedTabOverlay.class).getDataManager().removeDataHolder(dataHolder);
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public UUID getUniqueID() {
        return bukkitPlayer.getUniqueId();
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return bukkitPlayer;
    }

    @Override
    public <V> V get(@NonNull @Nonnull DataKey<V> key) {
        return dataHolder.get(key);
    }

    @Override
    public <T> void addDataChangeListener(@NonNull @Nonnull DataKey<T> key, @NonNull @Nonnull Runnable listener) {
        dataHolder.addDataChangeListener(key, listener);
    }

    @Override
    public <T> void removeDataChangeListener(@NonNull @Nonnull DataKey<T> key, @NonNull @Nonnull Runnable listener) {
        dataHolder.removeDataChangeListener(key, listener);
    }
}
