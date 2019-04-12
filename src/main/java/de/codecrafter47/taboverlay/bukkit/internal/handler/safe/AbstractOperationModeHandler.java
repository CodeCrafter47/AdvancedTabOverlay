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
