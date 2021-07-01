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

package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SafeTabOverlayHandlerFactory implements TabOverlayHandlerFactory {

    private final Map<Player, SafeTabOverlayHandler> tabOverlayHandlerMap = new HashMap<>();
    private PacketHelper packetHelper;

    public SafeTabOverlayHandlerFactory() throws IllegalStateException {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        int size = packet.getStrings().getFields().size();
        if (size == 5) {
            packetHelper = new PacketHelper1_8();
        } else if (size == 6) {
            packetHelper = new PacketHelper1_12();
        } else if (size == 3) {
            packetHelper = new PacketHelper1_13();
        } else if (size == 1) {
            packetHelper = new PacketHelper1_17();
        } else {
            throw new IllegalStateException("Unsupported Minecraft version");
        }
    }

    @Override
    public TabOverlayHandler create(Player player) {
        val channel = Util.getChannel(player);
        val tabOverlayHandler = new SafeTabOverlayHandler(player, packetHelper);
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
        for (SafeTabOverlayHandler handler : tabOverlayHandlerMap.values()) {
            handler.enterContentOperationMode(ContentOperationMode.PASS_TROUGH);
            handler.enterHeaderAndFooterOperationMode(HeaderAndFooterOperationMode.PASS_TROUGH);
        }
    }

    @RequiredArgsConstructor
    private static class MyChannelHandler extends ChannelOutboundHandlerAdapter {
        private final SafeTabOverlayHandler tablistHandler;

        private static final Set<PacketType> interceptedPacketTypes = ImmutableSet.of(PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.SCOREBOARD_TEAM);

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (PacketType.hasClass(msg.getClass())) {
                val packet = PacketContainer.fromPacket(msg);
                if (interceptedPacketTypes.contains(packet.getType())) {
                    PacketContainer clonedPacket = packet.shallowClone();
                    if (tablistHandler.onPacketSending(ctx, packet)) {
                        super.write(ctx, packet.getHandle(), promise);
                    }
                    tablistHandler.onPacketSent(ctx, clonedPacket);
                } else {
                    super.write(ctx, msg, promise);
                }

                tablistHandler.networkTick(ctx);
            } else {
                super.write(ctx, msg, promise);
            }
        }
    }
}
