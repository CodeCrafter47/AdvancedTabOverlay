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
