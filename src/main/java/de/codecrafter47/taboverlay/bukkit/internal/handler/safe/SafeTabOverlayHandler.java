package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import de.codecrafter47.taboverlay.handler.*;
import de.codecrafter47.taboverlay.util.Unchecked;
import io.netty.channel.ChannelHandlerContext;
import org.bukkit.entity.Player;

class SafeTabOverlayHandler implements TabOverlayHandler {

    static final WrappedChatComponent CHAT_COMPONENT_EMPTY = WrappedChatComponent.fromJson("{\"text\":\"\"}");
    private final Player player;

    private VanillaTabOverlayTracker tracker;
    private PacketHelper packetHelper;

    private HandlerQueue contentOperationModeHandlerQueue = new HandlerQueue(new NoOpOperationModeHandler());
    private HandlerQueue headerAndFooterOperationModeHandlerQueue = new HandlerQueue(new NoOpOperationModeHandler());

    private volatile boolean dirtyFlag = false;

    SafeTabOverlayHandler(Player player, PacketHelper packetHelper) {
        this.player = player;
        tracker = new VanillaTabOverlayTracker(packetHelper);
        this.packetHelper = packetHelper;
    }

    void setDirtyFlag() {
        dirtyFlag = true;
    }

    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        tracker.onPacketSending(packet, ctx);
        return getContentHandler().onPacketSending(ctx, packet)
                & getHeaderAndFooterHandler().onPacketSending(ctx, packet);
    }

    void onPacketSent(ChannelHandlerContext ctx, PacketContainer packet) {
        tracker.onPacketSent(ctx, packet);
    }

    void networkTick(ChannelHandlerContext ctx) {
        if (dirtyFlag) {
            dirtyFlag = false;

            contentOperationModeHandlerQueue.networkTick(ctx);
            headerAndFooterOperationModeHandlerQueue.networkTick(ctx);

            getContentHandler().networkTick(ctx);
            getHeaderAndFooterHandler().networkTick(ctx);
        }
    }

    private AbstractOperationModeHandler<?> getContentHandler() {
        return contentOperationModeHandlerQueue.getActiveHandler();
    }

    @Override
    public <R> R enterContentOperationMode(ContentOperationMode<R> operationMode) {
        AbstractOperationModeHandler newHandler;
        if (operationMode == ContentOperationMode.PASS_TROUGH) {
            newHandler = new ContentPassthroughOperationModeHandler(tracker);
        } else if (operationMode == ContentOperationMode.RECTANGULAR) {
            newHandler = new RectangularOperationModeHandler(this, tracker, player.getUniqueId(), packetHelper);
        } else if (operationMode == ContentOperationMode.SIMPLE) {
            newHandler = new SimpleOperationModeHandler(this, tracker, player.getUniqueId(), packetHelper);
        } else {
            throw new UnsupportedOperationException("Unsupported Operation mode " + operationMode.getName());
        }
        contentOperationModeHandlerQueue.addHandlerToQueue(newHandler);
        return Unchecked.cast(newHandler.getRepresentation());
    }

    private AbstractOperationModeHandler<?> getHeaderAndFooterHandler() {
        return headerAndFooterOperationModeHandlerQueue.getActiveHandler();
    }

    @Override
    public <R> R enterHeaderAndFooterOperationMode(HeaderAndFooterOperationMode<R> operationMode) {
        AbstractOperationModeHandler newHandler;
        if (operationMode == HeaderAndFooterOperationMode.PASS_TROUGH) {
            newHandler = new HeaderAndFooterPassthroughOperationModeHandler(tracker);
        } else if (operationMode == HeaderAndFooterOperationMode.CUSTOM) {
            newHandler = new CustomHeaderAndFooterOperationModeHandler(this);
        } else {
            throw new UnsupportedOperationException("Unsupported Operation mode " + operationMode.getName());
        }
        headerAndFooterOperationModeHandlerQueue.addHandlerToQueue(newHandler);
        return Unchecked.cast(newHandler.getRepresentation());
    }
}