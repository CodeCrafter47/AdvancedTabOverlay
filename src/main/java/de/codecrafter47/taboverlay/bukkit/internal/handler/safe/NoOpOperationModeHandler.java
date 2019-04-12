package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.events.PacketContainer;
import io.netty.channel.ChannelHandlerContext;

public class NoOpOperationModeHandler extends AbstractOperationModeHandler<Void> {

    @Override
    Void getRepresentation() {
        return null;
    }

    @Override
    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        return false;
    }
}
