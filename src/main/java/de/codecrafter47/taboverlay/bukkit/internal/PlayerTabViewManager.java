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

import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class PlayerTabViewManager {

    private static final String KEY = "ATO_TabView";

    private final AdvancedTabOverlay plugin;
    private final Logger logger;
    private final Executor updateExecutor;

    public PlayerTabViewManager(AdvancedTabOverlay plugin, Logger logger, Executor updateExecutor) {
        this.plugin = plugin;
        this.logger = logger;
        this.updateExecutor = updateExecutor;
    }

    public void removeFromPlayer(Player player) {
        player.removeMetadata(KEY, plugin);
    }

    public TabView get(Player player) {
        List<MetadataValue> list = player.getMetadata(KEY);
        if (list.isEmpty()) {
            TabView tabView = new TabView(logger, updateExecutor);
            player.setMetadata(KEY, new FixedMetadataValue(plugin, tabView));
            return tabView;
        } else {
            return ((TabView) list.get(0).value());
        }
    }
}
