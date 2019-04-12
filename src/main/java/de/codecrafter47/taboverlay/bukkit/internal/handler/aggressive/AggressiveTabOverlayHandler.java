package de.codecrafter47.taboverlay.bukkit.internal.handler.aggressive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.internal.util.Util;
import de.codecrafter47.taboverlay.handler.*;
import de.codecrafter47.taboverlay.util.Unchecked;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// todo this currently breaks spectator mode players going trough walls
@RequiredArgsConstructor
class AggressiveTabOverlayHandler implements TabOverlayHandler {

    private static final WrappedChatComponent CHAT_COMPONENT_EMPTY = WrappedChatComponent.fromJson("{\"text\":\"\"}");
    private final Player player;

    boolean active = false;
    PassthroughPriorFirstMoveHandler handler = null;
    private OperationModeHandler<?> contentOperationModeHandler = new PassthroughOperationModeHandler();
    private OperationModeHandler<?> headerAndFooterOperationModeHandler = new PassthroughOperationModeHandler();

    private final static Map<Integer, RectangularTabOverlay.Dimension> SUPPORTED_DIMENSIONS;

    private static int TABLIST_PLAYER_REMOVE_DELAY = 5;

    private static String[] fakePlayerUsernames = IntStream.range(0, 80)
            .mapToObj(n -> String.format(" Tab Slot %02d", n))
            .toArray(String[]::new);
    private static UUID[] fakePlayerUUIDs = Arrays.stream(fakePlayerUsernames)
            .map(name -> UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)))
            .toArray(UUID[]::new);
    private static Set<UUID> fakePlayerUUIDSet = ImmutableSet.copyOf(fakePlayerUUIDs);

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

    void tick() {
        contentOperationModeHandler.tick();
        headerAndFooterOperationModeHandler.tick();
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

    private abstract class OperationModeHandler<R> {

        boolean valid;

        abstract R getRepresentation();

        abstract boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet);

        void onActivated() {
            valid = true;
        }

        void onDeactivated() {
            valid = false;
        }

        void tick() {

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
            // add all players to the tab list
            val list = Bukkit.getOnlinePlayers().stream()
                    .map(Util::getPlayerInfoData)
                    .collect(Collectors.toList());
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.getPlayerInfoDataLists().write(0, list);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
    }

    private final class HeaderAndFooterPassthroughOperationModeHandler extends OperationModeHandler<Void> {

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
                this.header = header != null ? WrappedChatComponent.fromText(header) : CHAT_COMPONENT_EMPTY;
                this.footer = footer != null ? WrappedChatComponent.fromText(footer) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
            }

            @Override
            public void setHeaderFooter(@Nullable String header, @Nullable String footer, char alternateColorChar) {
                setHeaderFooter(header != null ? ChatColor.translateAlternateColorCodes(alternateColorChar, header) : null,
                        footer != null ? ChatColor.translateAlternateColorCodes(alternateColorChar, footer) : null);
            }

            @Override
            public void setHeader(@Nullable String header) {
                this.header = header != null ? WrappedChatComponent.fromText(header) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
            }

            @Override
            public void setHeader(@Nullable String header, char alternateColorChar) {
                setHeader(header != null ? ChatColor.translateAlternateColorCodes(alternateColorChar, header) : null);
            }

            @Override
            public void setFooter(@Nullable String footer) {
                this.footer = footer != null ? WrappedChatComponent.fromText(footer) : CHAT_COMPONENT_EMPTY;
                updateHeaderFooter();
            }

            @Override
            public void setFooter(@Nullable String footer, char alternateColorChar) {
                setFooter(footer != null ? ChatColor.translateAlternateColorCodes(alternateColorChar, footer) : null);
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

    private abstract class CustomOperationModeHandler<R extends CustomOperationModeHandler.Representation> extends OperationModeHandler<R> {

        final Map<UUID, Integer> removePlayers = new LinkedHashMap<>();
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

            if (type == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                val uuid = packet.getSpecificModifier(UUID.class).read(0);

                addBackPlayerEntry(ctx, uuid);

                removePlayers.put(uuid, TABLIST_PLAYER_REMOVE_DELAY);
            } else if (type == PacketType.Play.Server.RESPAWN) {
                val uuid = player.getUniqueId();

                addBackPlayerEntry(ctx, uuid);

                removePlayers.put(uuid, TABLIST_PLAYER_REMOVE_DELAY);
                active = false;
                if (handler != null) {
                    handler.update();
                }
            } else if (type == PacketType.Play.Server.PLAYER_INFO) {
                val infoDataList = packet.getPlayerInfoDataLists().read(0);
                if (!infoDataList.isEmpty() && !fakePlayerUUIDSet.contains(infoDataList.get(0).getProfile().getUUID())) {
                    val action = packet.getPlayerInfoAction().read(0);
                    if (action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
                        for (PlayerInfoData item : infoDataList) {
                            removePlayers.put(item.getProfile().getUUID(), TABLIST_PLAYER_REMOVE_DELAY);
                        }
                    } else if (action == EnumWrappers.PlayerInfoAction.REMOVE_PLAYER) {
                        for (PlayerInfoData item : infoDataList) {
                            removePlayers.remove(item.getProfile().getUUID());
                        }
                    } else if (action == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE) {
                        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                        for (PlayerInfoData item : infoDataList) {
                            removePlayers.put(item.getProfile().getUUID(), TABLIST_PLAYER_REMOVE_DELAY);
                        }
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        private void addBackPlayerEntry(ChannelHandlerContext ctx, UUID uuid) {
            if (!removePlayers.containsKey(uuid)) {
                // send tab list item
                val addPlayerPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                addPlayerPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                val player1 = Bukkit.getPlayer(uuid);
                addPlayerPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(WrappedGameProfile.fromPlayer(player1), 0, EnumWrappers.NativeGameMode.fromBukkit(player1.getGameMode()), null)));
                ctx.write(addPlayerPacket.getHandle());
            }
        }

        @Override
        void onActivated() {
            super.onActivated();
            for (Player p : Bukkit.getOnlinePlayers()) {
                removePlayers.put(p.getUniqueId(), TABLIST_PLAYER_REMOVE_DELAY);
            }
        }

        @Override
        @SneakyThrows
        void onDeactivated() {
            super.onDeactivated();
            if (size > 0) {
                val list = Arrays.stream(fakePlayerUUIDs, 0, size)
                        .map(uuid -> new PlayerInfoData(new WrappedGameProfile(uuid, ""), 0, EnumWrappers.NativeGameMode.SURVIVAL, null))
                        .collect(Collectors.toList());
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                packet.getPlayerInfoDataLists().write(0, list);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                size = 0;
            }
            representation.valid = false;
        }

        @Override
        @SneakyThrows
        void tick() {
            if (active) {
                List<PlayerInfoData> list = new ArrayList<>();
                for (val entry : removePlayers.entrySet()) {
                    entry.setValue(entry.getValue() - 1);
                    if (entry.getValue() <= 0) {
                        list.add(new PlayerInfoData(new WrappedGameProfile(entry.getKey(), null), 0, EnumWrappers.NativeGameMode.NOT_SET, null));
                    }
                }
                removePlayers.values().removeIf(value -> value < 0);

                if (!list.isEmpty()) {
                    val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                    packet.getPlayerInfoDataLists().write(0, list);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                }
            }
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
                            .map(uuid -> new PlayerInfoData(new WrappedGameProfile(uuid, ""), 0, EnumWrappers.NativeGameMode.SURVIVAL, null))
                            .collect(Collectors.toList());
                    val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                    packet.getPlayerInfoDataLists().write(0, list);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    size = 0;
                }
                CustomOperationModeHandler.this.size = size;
            }

            @SneakyThrows
            void updateSlot(int index) {
                val icon = icons[index];
                val text = texts[index];
                int ping = pings[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                if (icon.hasTextureProperty()) {
                    profile.getProperties().put("textures", new WrappedSignedProperty(icon.getTextureProperty().getName(),
                            icon.getTextureProperty().getValue(), icon.getTextureProperty().getSignature()));
                }
                val data = new PlayerInfoData(profile, ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(text));
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }

            @SneakyThrows
            void updateSlotText(int index) {
                String text = texts[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                val data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(text));
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
                packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }

            @SneakyThrows
            void updateSlotPing(int index) {
                int ping = pings[index];
                val profile = new WrappedGameProfile(fakePlayerUUIDs[index], fakePlayerUsernames[index]);
                val data = new PlayerInfoData(profile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);
                val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
                packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
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

        class Representation extends CustomOperationModeHandler.Representation implements RectangularTabOverlay {

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
                texts[index] = text;
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
            public void setSlot(int column, int row, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
                setSlot(column, row, uuid, icon, ChatColor.translateAlternateColorCodes(alternateColorChar, text), ping);
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
                texts[index] = text;
                updateSlotText(index);
            }

            @Override
            public void setText(int column, int row, @Nonnull String text, char alternateColorChar) {
                setText(column, row, ChatColor.translateAlternateColorCodes(alternateColorChar, text));
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

        class Representation extends CustomOperationModeHandler.Representation implements SimpleTabOverlay {

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
                texts[index] = text;
                icons[index] = icon;
                pings[index] = ping;
                updateSlot(index);
            }

            @Override
            public void setSlot(int index, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
                setSlot(index, icon, ChatColor.translateAlternateColorCodes(alternateColorChar, text), ping);
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
                texts[index] = text;
                updateSlotText(index);
            }

            @Override
            public void setText(int index, @Nonnull String text, char alternateColorChar) {
                setText(index, ChatColor.translateAlternateColorCodes(alternateColorChar, text));
            }

            @Override
            public void setPing(int index, int ping) {
                pings[index] = ping;
                updateSlotPing(index);
            }
        }
    }
}