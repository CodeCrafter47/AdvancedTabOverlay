package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.taboverlay.AbstractTabOverlayProvider;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.handler.OperationMode;
import de.codecrafter47.taboverlay.handler.TabOverlay;

public class PassthroughPriorFirstMoveHandler extends AbstractTabOverlayProvider<TabOverlay> {
    private final DefaultTabOverlayHandler handler;
    private final TabView tabView;

    protected PassthroughPriorFirstMoveHandler(DefaultTabOverlayHandler handler, TabView tabView) {
        super("passthrough-before-first-move", Integer.MAX_VALUE, OperationMode.PASS_TROUGH);
        this.handler = handler;
        this.tabView = tabView;
    }

    @Override
    protected void activate(TabView tabView, TabOverlay tabOverlay) {

    }

    @Override
    protected void attach(TabView tabView) {

    }

    @Override
    protected void detach(TabView tabView) {

    }

    @Override
    protected void deactivate(TabView tabView) {

    }

    @Override
    protected boolean shouldActivate(TabView tabView) {
        return !handler.active;
    }

    void update() {
        tabView.getTabOverlayProviders().scheduleUpdate();
    }
}
