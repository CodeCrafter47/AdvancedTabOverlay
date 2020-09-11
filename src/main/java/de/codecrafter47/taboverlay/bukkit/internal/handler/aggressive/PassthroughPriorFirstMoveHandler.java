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

package de.codecrafter47.taboverlay.bukkit.internal.handler.aggressive;

import de.codecrafter47.taboverlay.AbstractTabOverlayProvider;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.handler.ContentOperationMode;
import de.codecrafter47.taboverlay.handler.HeaderAndFooterOperationMode;
import de.codecrafter47.taboverlay.handler.TabOverlayHandle;

class PassthroughPriorFirstMoveHandler extends AbstractTabOverlayProvider<TabOverlayHandle, TabOverlayHandle> {
    private final AggressiveTabOverlayHandler handler;
    private final TabView tabView;

    protected PassthroughPriorFirstMoveHandler(AggressiveTabOverlayHandler handler, TabView tabView) {
        super("passthrough-before-first-move", Integer.MAX_VALUE, ContentOperationMode.PASS_TROUGH, HeaderAndFooterOperationMode.PASS_TROUGH);
        this.handler = handler;
        this.tabView = tabView;
    }

    @Override
    protected void activate(TabView tabView, TabOverlayHandle contentHandle, TabOverlayHandle headerAndFooterHandle) {

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
