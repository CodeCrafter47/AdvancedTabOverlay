package de.codecrafter47.taboverlay.bukkit;

import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.handler.TabOverlayHandler;
import org.bukkit.entity.Player;

public interface TabOverlayHandlerFactory {

    TabOverlayHandler create(Player player);

    void onCreated(TabView tabView, Player player);

    void onDisable();
}
