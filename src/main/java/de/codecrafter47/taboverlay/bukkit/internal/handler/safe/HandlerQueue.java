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

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class HandlerQueue {

    private final Queue<AbstractOperationModeHandler<?>> nextActiveHandlerQueue = new ConcurrentLinkedQueue<>();

    @Getter
    private AbstractOperationModeHandler<?> activeHandler;

    HandlerQueue(AbstractOperationModeHandler<?> activeHandler) {
        this.activeHandler = activeHandler;
    }

    void addHandlerToQueue(AbstractOperationModeHandler<?> handler) {
        nextActiveHandlerQueue.add(handler);
    }

    void networkTick(ChannelHandlerContext ctx) {
        AbstractOperationModeHandler<?> handler;
        while (null != (handler = nextActiveHandlerQueue.poll())) {
            this.activeHandler.onDeactivated(ctx);
            handler.onActivated(this.activeHandler, ctx);
            this.activeHandler = handler;
        }
    }
}
