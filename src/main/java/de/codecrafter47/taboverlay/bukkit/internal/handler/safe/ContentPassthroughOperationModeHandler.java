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
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.val;

import java.util.List;
import java.util.stream.Collectors;

final class ContentPassthroughOperationModeHandler extends AbstractOperationModeHandler<Void> {

    private final VanillaTabOverlayTracker tracker;

    ContentPassthroughOperationModeHandler(VanillaTabOverlayTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    Void getRepresentation() {
        return null;
    }

    @Override
    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        return true; // pass packets to client
    }

    @Override
    @SneakyThrows
    void onActivated(AbstractOperationModeHandler<?> previous, ChannelHandlerContext ctx) {
        super.onActivated(previous, ctx);

        // fix all player list entries by sending them again
        List<PlayerInfoData> list = tracker.getPlayerListEntries()
                .stream()
                .map(entry -> new PlayerInfoData(entry.profile, entry.latency, entry.gameMode, entry.displayName))
                .collect(Collectors.toList());
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet.getPlayerInfoDataLists().write(0, list);
        ctx.write(packet.getHandle(), ctx.newPromise());
    }
}
