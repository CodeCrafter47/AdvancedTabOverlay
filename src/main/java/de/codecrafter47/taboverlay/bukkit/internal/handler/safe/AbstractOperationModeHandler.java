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

import com.comphenix.protocol.events.PacketContainer;
import io.netty.channel.ChannelHandlerContext;

abstract class AbstractOperationModeHandler<R> {

    boolean valid = true;

    abstract R getRepresentation();

    abstract boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet);

    void onActivated(AbstractOperationModeHandler<?> previous, ChannelHandlerContext ctx) {
    }

    void onDeactivated(ChannelHandlerContext ctx) {
        valid = false;
    }

    void networkTick(ChannelHandlerContext ctx) {

    }

}
