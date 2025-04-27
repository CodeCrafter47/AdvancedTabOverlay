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

package de.codecrafter47.taboverlay.bukkit.internal.handler.ordered;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.google.common.collect.ImmutableSet;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.bukkit.TabOverlayHandlerFactory;
import de.codecrafter47.taboverlay.bukkit.internal.util.Util;
import de.codecrafter47.taboverlay.handler.ContentOperationMode;
import de.codecrafter47.taboverlay.handler.HeaderAndFooterOperationMode;
import de.codecrafter47.taboverlay.handler.TabOverlayHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OrderedTabOverlayHandlerFactory implements TabOverlayHandlerFactory, Listener {

    private final Map<Player, OrderedTabOverlayHandler> tabOverlayHandlerMap = new HashMap<>();

    public OrderedTabOverlayHandlerFactory() {
    }

    @Override
    public TabOverlayHandler create(Player player) {
        val channel = Util.getChannel(player);
        val tabOverlayHandler = new OrderedTabOverlayHandler(player);
        val channelHandler = new MyChannelHandler(tabOverlayHandler);
        channel.pipeline().addBefore("packet_handler", "AdvancedTabOverlay-0", channelHandler);
        tabOverlayHandlerMap.put(player, tabOverlayHandler);
        return tabOverlayHandler;
    }

    @Override
    public void onCreated(TabView tabView, Player player) {
    }

    @Override
    public void onDisable() {
        for (OrderedTabOverlayHandler handler : tabOverlayHandlerMap.values()) {
            handler.enterContentOperationMode(ContentOperationMode.PASS_TROUGH);
            handler.enterHeaderAndFooterOperationMode(HeaderAndFooterOperationMode.PASS_TROUGH);
        }
    }

    @RequiredArgsConstructor
    private static class MyChannelHandler extends ChannelOutboundHandlerAdapter {
        private final OrderedTabOverlayHandler tablistHandler;

        private static final Set<PacketType> interceptedPacketTypes = ImmutableSet.of(PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (PacketRegistry.getPacketType(msg.getClass()) != null) {
                val packet = PacketContainer.fromPacket(msg);
                if (interceptedPacketTypes.contains(packet.getType())) {
                    if (tablistHandler.onPacketSending(ctx, packet)) {
                        super.write(ctx, packet.getHandle(), promise);
                    }
                    return;
                }
            }
            super.write(ctx, msg, promise);
        }
    }
}
