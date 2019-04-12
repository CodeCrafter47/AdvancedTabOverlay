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
