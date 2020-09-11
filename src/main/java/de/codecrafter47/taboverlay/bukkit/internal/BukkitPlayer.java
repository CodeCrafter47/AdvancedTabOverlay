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
