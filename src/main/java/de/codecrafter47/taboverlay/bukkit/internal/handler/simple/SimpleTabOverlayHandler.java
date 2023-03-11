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

package de.codecrafter47.taboverlay.bukkit.internal.handler.simple;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.internal.util.Util;
import de.codecrafter47.taboverlay.config.misc.ChatFormat;
import de.codecrafter47.taboverlay.config.misc.Unchecked;
import de.codecrafter47.taboverlay.handler.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// todo this currently breaks spectator mode players going trough walls
@RequiredArgsConstructor
class SimpleTabOverlayHandler implements TabOverlayHandler {

    private static final WrappedChatComponent CHAT_COMPONENT_EMPTY = WrappedChatComponent.fromJson("{\"text\":\"\"}");
    private final Player player;

    private OperationModeHandler<?> contentOperationModeHandler = new PassthroughOperationModeHandler();
    private OperationModeHandler<?> headerAndFooterOperationModeHandler = new PassthroughOperationModeHandler();

    private final static Map<Integer, RectangularTabOverlay.Dimension> SUPPORTED_DIMENSIONS;

    private static final String[] fakePlayerUsernames = IntStream.range(0, 80)
            .mapToObj(n -> String.format(" Tab Slot %02d", n))
            .toArray(String[]::new);
    private static final UUID[] fakePlayerUUIDs = Arrays.stream(fakePlayerUsernames)
            .map(name -> UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)))
            .toArray(UUID[]::new);
    private static final Set<UUID> fakePlayerUUIDSet = ImmutableSet.copyOf(fakePlayerUUIDs);

    static {
        val builder = ImmutableMap.<Integer, RectangularTabOverlay.Dimension>builder();
        for (int columns = 1; columns <= 4; columns++) {
            for (int rows = columns == 1 ? 0 : 1; rows <= 20; rows++) {
                if (columns != 1 && rows != 0 && columns * rows <= (columns - 1) * 20)
                    continue;
                builder.put(columns * rows, new RectangularTabOverlay.Dimension(columns, rows));
            }
        }
        SUPPORTED_DIMENSIONS = builder.build();
    }

    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        return contentOperationModeHandler.onPacketSending(ctx, packet)
                & headerAndFooterOperationModeHandler.onPacketSending(ctx, packet);
    }

    @Override
    public <R> R enterContentOperationMode(ContentOperationMode<R> operationMode) {
        OperationModeHandler newHandler;
        if (operationMode == ContentOperationMode.PASS_TROUGH) {
            newHandler = new PassthroughOperationModeHandler();
        } else if (operationMode == ContentOperationMode.RECTANGULAR) {
            newHandler = new FixedSizeOperationModeHandler();
        } else if (operationMode == ContentOperationMode.SIMPLE) {
            newHandler = new SimpleOperationModeHandler();
        } else {
            throw new UnsupportedOperationException("Unsupported Operation mode " + operationMode.getName());
        }
        contentOperationModeHandler.onDeactivated();
        newHandler.onActivated();
        contentOperationModeHandler = newHandler;
        return Unchecked.cast(contentOperationModeHandler.getRepresentation());
    }

    @Override
    public <R> R enterHeaderAndFooterOperationMode(HeaderAndFooterOperationMode<R> operationMode) {
        OperationModeHandler newHandler;
        if (operationMode == HeaderAndFooterOperationMode.PASS_TROUGH) {
            newHandler = new HeaderAndFooterPassthroughOperationModeHandler();
        } else if (operationMode == HeaderAndFooterOperationMode.CUSTOM) {
            newHandler = new CustomHeaderAndFooterOperationModeHandler();
        } else {
            throw new UnsupportedOperationException("Unsupported Operation mode " + operationMode.getName());
        }
        headerAndFooterOperationModeHandler.onDeactivated();
        newHandler.onActivated();
        headerAndFooterOperationModeHandler = newHandler;
        return Unchecked.cast(headerAndFooterOperationModeHandler.getRepresentation());
    }

    private abstract static class OperationModeHandler<R> {

        boolean valid;

        abstract R getRepresentation();

        abstract boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet);

        void onActivated() {
            valid = true;
        }

        void onDeactivated() {
            valid = false;
        }

    }

    private final class PassthroughOperationModeHandler extends OperationModeHandler<Void> {

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
        void onActivated() {
            super.onActivated();
            // show all players on the tab list
            val list = Bukkit.getOnlinePlayers().stream()
                    .map(Util::getPlayerInfoData)
                    .collect(Collectors.toList());
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_LISTED));
            packet.getPlayerInfoDataLists().write(1, list);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
    }

    private static final class HeaderAndFooterPassthroughOperationModeHandler extends OperationModeHandler<Void> {

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
        void onActivated() {
            super.onActivated();
            // todo we'd need to restore the vanilla header here.
        }
    }

    private final class CustomHeaderAndFooterOperationModeHandler extends OperationModeHandler<HeaderAndFooterHandle> {
        Representation representation = new Representation();

        @Override
        HeaderAndFooterHandle getRepresentation() {
            return representation;
        }

        @Override
        boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
            // todo can't block those cause we'd block our own packets.
            //return packet.getType() != PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER;
            return true;
        }

        @Override
        void onDeactivated() {
            representation.valid = false;
            super.onDeactivated();
        }

        class Representation implements HeaderAndFooterHandle {

            boolean valid = true;
            WrappedChatComponent header = CHAT_COMPONENT_EMPTY;
            WrappedChatComponent footer = CHAT_COMPONENT_EMPTY;

            @SneakyThrows
            private void updateHeaderFooter() {
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);
                packet.getChatComponents().write(0, this.header);
                packet.getChatComponents().write(1, this.footer);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }

            @Override
            public void setHeaderFooter(@Nullable String header, @Nullable String footer) {
                this.header = header != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(header)) : CHAT_COMPONENT_EMPTY;
                this.footer = footer != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(footer)) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
            }

            @Override
            public void setHeader(@Nullable String header) {
                this.header = header != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(header)) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
            }

            @Override
            public void setFooter(@Nullable String footer) {
                this.footer = footer != null ? WrappedChatComponent.fromJson(ChatFormat.formattedTextToJson(footer)) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
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

    private abstract class CustomOperationModeHandler<R extends CustomOperationModeHandler<R>.Representation> extends OperationModeHandler<R> {

        final R representation;
        int size = 0;

        CustomOperationModeHandler() {
            this.representation = createRepresentation();
        }

        @Override
        R getRepresentation() {
            return representation;
        }

        abstract R createRepresentation();

        @Override
        boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {

            val type = packet.getType();

            if (type == PacketType.Play.Server.PLAYER_INFO) {
                val infoDataList = packet.getPlayerInfoDataLists().read(1);
                if (!infoDataList.isEmpty() && !fakePlayerUUIDSet.contains(infoDataList.get(0).getProfile().getUUID())) {
                    val action = packet.getPlayerInfoActions().read(0);
                    if (action.contains(EnumWrappers.PlayerInfoAction.UPDATE_LISTED)) {
                        packet.getPlayerInfoDataLists().write(1, infoDataList.stream().map(playerInfoData -> new PlayerInfoData(playerInfoData.getProfileId(), playerInfoData.getLatency(), false, playerInfoData.getGameMode(), playerInfoData.getProfile(), playerInfoData.getDisplayName(), playerInfoData.getProfileKeyData())).collect(Collectors.toList()));
                    }
                }
            }
            return true;
        }

        @Override
        void onActivated() {
            super.onActivated();
            // hide all players from the tab list
            val list = Bukkit.getOnlinePlayers().stream()
                    .map(Util::getPlayerInfoData)
                    .map(playerInfoData -> new PlayerInfoData(playerInfoData.getProfileId(), playerInfoData.getLatency(), false, playerInfoData.getGameMode(), playerInfoData.getProfile(), playerInfoData.getDisplayName(), playerInfoData.getProfileKeyData()))
                    .collect(Collectors.toList());
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_LISTED));
            packet.getPlayerInfoDataLists().write(1, list);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }

        @Override
        @SneakyThrows
        void onDeactivated() {
            super.onDeactivated();
            if (size > 0) {
                val list = Arrays.stream(fakePlayerUUIDs, 0, size)
                        .collect(Collectors.toList());;
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                packet.getUUIDLists().write(0, list);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                size = 0;
            }
            representation.valid = false;
        }

        class Representation implements TabOverlayHandle.BatchModifiable, TabOverlayHandle {

            boolean valid = true;
            Icon[] icons;
            String[] texts;
            int[] pings;

            {
                icons = new Icon[80];
                Arrays.fill(icons, Icon.DEFAULT_STEVE);
                texts = new String[80];
                Arrays.fill(texts, "");
                pings = new int[80];
                Arrays.fill(pings, 0);
            }

            @SneakyThrows
            void setSize(int size) {
                if (size < CustomOperationModeHandler.this.size) {
                    val list = Arrays.stream(fakePlayerUUIDs, size, CustomOperationModeHandler.this.size)
                            .collect(Collectors.toList());
                    val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                    packet.getUUIDLists().write(0, list);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    size = 0;
                }
                CustomOperationModeHandler.this.size = size;
            }

            @SneakyThrows
            void updateSlot(int index) {
                val packetRemove = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                packetRemove.getUUIDLists().write(0, Collections.singletonList(fakePlayerUUIDs[index]));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetRemove);
                val icon = icons[index];
                val text = texts[index];
                int ping = pings[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                if (icon.hasTextureProperty()) {
                    profile.getProperties().put("textures", new WrappedSignedProperty(icon.getTextureProperty().getName(),
                            icon.getTextureProperty().getValue(), icon.getTextureProperty().getSignature()));
                }
                val data = new PlayerInfoData(profile, ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromJson(text));
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoActions().write(0, ImmutableSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY, EnumWrappers.PlayerInfoAction.UPDATE_LISTED));
                packet.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }

            @SneakyThrows
            void updateSlotText(int index) {
                String text = texts[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                val data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromJson(text));
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoActions().write(0, Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME));
                packet.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }

            @SneakyThrows
            void updateSlotPing(int index) {
                int ping = pings[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                val data = new PlayerInfoData(profile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoActions().write(0, Collections.singleton(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY));
                packet.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
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

    private final class FixedSizeOperationModeHandler extends CustomOperationModeHandler<FixedSizeOperationModeHandler.Representation> {

        @Override
        Representation createRepresentation() {
            return new Representation();
        }

        class Representation extends CustomOperationModeHandler<FixedSizeOperationModeHandler.Representation>.Representation implements RectangularTabOverlay {

            @Override
            public Dimension getSize() {
                int size = FixedSizeOperationModeHandler.this.size;
                return SUPPORTED_DIMENSIONS.get(size);
            }

            @Override
            public Collection<Dimension> getSupportedSizes() {
                return SUPPORTED_DIMENSIONS.values();
            }

            @Override
            public void setSize(@Nonnull Dimension size) {
                setSize(size.getSize());
            }

            @Override
            public void setSlot(int column, int row, UUID uuid, @Nonnull Icon icon, @Nonnull String text, int ping) {
                int index = getIndex(column, row);
                texts[index] = ChatFormat.formattedTextToJson(text);
                icons[index] = icon;
                pings[index] = ping;
                updateSlot(index);
            }

            private int getIndex(int column, int row) {
                int columns = (size + 19) / 20;
                int rows = (size + columns - 1) / columns;
                int index = column * rows + row;
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException();
                }
                return index;
            }

            @Override
            public void setUuid(int column, int row, UUID uuid) {
                // nothing to do as this TabOverlayHandler doesn't use the uuid
            }

            @Override
            public void setIcon(int column, int row, @Nonnull Icon icon) {
                int index = getIndex(column, row);
                icons[index] = icon;
                updateSlot(index);
            }

            @Override
            public void setText(int column, int row, @Nonnull String text) {
                int index = getIndex(column, row);
                texts[index] = ChatFormat.formattedTextToJson(text);
                updateSlotText(index);
            }

            @Override
            public void setPing(int column, int row, int ping) {
                int index = getIndex(column, row);
                pings[index] = ping;
                updateSlotPing(index);
            }

        }
    }


    private final class SimpleOperationModeHandler extends CustomOperationModeHandler<SimpleOperationModeHandler.Representation> {

        @Override
        Representation createRepresentation() {
            return new Representation();
        }

        class Representation extends CustomOperationModeHandler<SimpleOperationModeHandler.Representation>.Representation implements SimpleTabOverlay {

            @Override
            public void setSize(int size) {
                super.setSize(size);
            }

            @Override
            public int getSize() {
                return size;
            }

            @Override
            public int getMaxSize() {
                return 80;
            }

            @Override
            public void setSlot(int index, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, int ping) {
                texts[index] = ChatFormat.formattedTextToJson(text);
                icons[index] = icon;
                pings[index] = ping;
                updateSlot(index);
            }

            @Override
            public void setUuid(int index, @Nullable UUID uuid) {
                // nothing to do
            }

            @Override
            public void setIcon(int index, @Nonnull Icon icon) {
                icons[index] = icon;
                updateSlot(index);
            }

            @Override
            public void setText(int index, @Nonnull String text) {
                texts[index] = ChatFormat.formattedTextToJson(text);
                updateSlotText(index);
            }

            @Override
            public void setPing(int index, int ping) {
                pings[index] = ping;
                updateSlotPing(index);
            }
        }
    }
}