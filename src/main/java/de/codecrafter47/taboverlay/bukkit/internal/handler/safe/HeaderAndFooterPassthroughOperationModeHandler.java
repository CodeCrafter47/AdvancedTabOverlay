package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.val;

final class HeaderAndFooterPassthroughOperationModeHandler extends AbstractOperationModeHandler<Void> {
    private final VanillaTabOverlayTracker tracker;

    HeaderAndFooterPassthroughOperationModeHandler(VanillaTabOverlayTracker tracker) {
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

        // restore header
        WrappedChatComponent header = tracker.getHeader();
        WrappedChatComponent footer = tracker.getFooter();
        if (header == null) {
            header = SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
        }
        if (footer == null) {
            footer = SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
        }
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
        packet.getChatComponents().write(0, header);
        packet.getChatComponents().write(1, footer);
        ctx.write(packet.getHandle(), ctx.newPromise());
    }
}
