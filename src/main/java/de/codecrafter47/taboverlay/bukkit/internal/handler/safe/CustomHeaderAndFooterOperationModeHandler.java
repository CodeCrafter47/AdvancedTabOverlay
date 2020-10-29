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
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import de.codecrafter47.taboverlay.config.misc.ChatFormat;
import de.codecrafter47.taboverlay.handler.HeaderAndFooterHandle;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.Objects;

final class CustomHeaderAndFooterOperationModeHandler extends AbstractOperationModeHandler<HeaderAndFooterHandle> {
    private SafeTabOverlayHandler safeTabOverlayHandler;
    private Representation representation = new Representation();

    private volatile boolean dirty;

    CustomHeaderAndFooterOperationModeHandler(SafeTabOverlayHandler safeTabOverlayHandler) {
        this.safeTabOverlayHandler = safeTabOverlayHandler;
    }

    @Override
    HeaderAndFooterHandle getRepresentation() {
        return representation;
    }

    @Override
    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        return packet.getType() != PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER;
    }

    @Override
    void onDeactivated(ChannelHandlerContext ctx) {
        representation.valid = false;
        super.onDeactivated(ctx);
    }

    @Override
    void networkTick(ChannelHandlerContext ctx) {
        if (dirty) {
            dirty = false;

            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
            packet.getChatComponents().write(0, representation.header);
            packet.getChatComponents().write(1, representation.footer);
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
    }

    @SneakyThrows
    private void markDirty() {
        dirty = true;
        safeTabOverlayHandler.setDirtyFlag();
    }

    class Representation implements HeaderAndFooterHandle {

        boolean valid = true;
        WrappedChatComponent header = SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
        WrappedChatComponent footer = SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;

        @Override
        public void setHeaderFooter(@Nullable String header, @Nullable String footer) {
            WrappedChatComponent headerComponent = header != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(header)) : SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
            WrappedChatComponent footerComponent = footer != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(footer)) : SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
            if (!Objects.equals(this.header, headerComponent) || !Objects.equals(this.footer, footerComponent)) {
                this.header = headerComponent;
                this.footer = footerComponent;
                markDirty();
            }
        }

        @Override
        public void setHeader(@Nullable String header) {
            WrappedChatComponent headerComponent = header != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(header)) : SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
            if (!Objects.equals(this.header, headerComponent)) {
                this.header = headerComponent;
                markDirty();
            }
        }

        @Override
        public void setFooter(@Nullable String footer) {
            WrappedChatComponent footerComponent = footer != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(footer)) : SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY;
            if (!Objects.equals(this.footer, footerComponent)) {
                this.footer = footerComponent;
                markDirty();
            }
        }

        @Override
        public void beginBatchModification() {
            // do nothing
        }

        @Override
        public void completeBatchModification() {
            // do nothing
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }
}
