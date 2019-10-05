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
