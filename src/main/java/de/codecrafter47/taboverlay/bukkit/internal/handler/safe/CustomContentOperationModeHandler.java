package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.ProfileProperty;
import de.codecrafter47.taboverlay.bukkit.internal.util.BitSet;
import de.codecrafter47.taboverlay.bukkit.internal.util.ConcurrentBitSet;
import de.codecrafter47.taboverlay.bukkit.internal.util.FastChat;
import de.codecrafter47.taboverlay.handler.TabOverlayHandle;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.val;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

abstract class CustomContentOperationModeHandler<R extends CustomContentOperationModeHandler.Representation> extends AbstractOperationModeHandler<R> implements VanillaTabOverlayTracker.PlayerListEventListener {

    private SafeTabOverlayHandler handler;
    final R representation;
    private volatile boolean dirty = false;
    private PacketHelper packetHelper;
    private final VanillaTabOverlayTracker tracker;
    private final TeamManager teamManager;
    private final UUID viewerUuid;

    @Nonnull
    BitSet usedSlots;
    BitSet dirtySlots;
    private boolean using80Slots;
    private int usedSlotsCount;
    private final SlotState[] slotState;
    private final UUID[] slotUuid;
    private final String[] slotUsername;
    private boolean viewerIsSpectator = false;
    int highestUsedSlotIndex;
    final Map<UUID, Integer> playerUuidToSlotMap;
    final Set<UUID> freePlayers;
    boolean canShrink = false;

    CustomContentOperationModeHandler(SafeTabOverlayHandler handler, VanillaTabOverlayTracker tracker, UUID viewerUuid, PacketHelper packetHelper) {
        this.handler = handler;
        this.tracker = tracker;
        this.viewerUuid = viewerUuid;
        this.representation = createRepresentation();
        this.teamManager = new TeamManager(tracker, packetHelper);
        this.dirtySlots = new BitSet(80);
        this.usedSlots = new BitSet(80);
        this.usedSlotsCount = 0;
        this.using80Slots = false;
        this.slotState = new SlotState[80];
        Arrays.fill(this.slotState, SlotState.UNUSED);
        this.slotUuid = new UUID[80];
        this.slotUsername = new String[80];
        this.highestUsedSlotIndex = -1;
        this.playerUuidToSlotMap = new HashMap<>();
        this.freePlayers = new HashSet<>();
        this.packetHelper = packetHelper;
    }

    @Override
    R getRepresentation() {
        return representation;
    }

    abstract R createRepresentation();

    abstract void updateSize();

    @Override
    boolean onPacketSending(ChannelHandlerContext ctx, PacketContainer packet) {
        PacketType type = packet.getType();
        if (type == PacketType.Play.Server.SCOREBOARD_TEAM) {
            teamManager.onTeamPacket(packet);
        } else if (type == PacketType.Play.Server.PLAYER_INFO) {
            // let citizens pass
            List<PlayerInfoData> list = packet.getPlayerInfoDataLists().read(0);
            List<PlayerInfoData> prunedList = list.stream().filter(entry -> entry.getProfile().getUUID().version() == 2).collect(Collectors.toList());
            if (prunedList.isEmpty()) {
                return false;
            } else {
                packet.getPlayerInfoDataLists().write(0, prunedList);
            }
        }
        return true;
    }

    @Override
    void onActivated(AbstractOperationModeHandler<?> previous, ChannelHandlerContext ctx) {
        super.onActivated(previous, ctx);
        teamManager.activate(ctx);

        // switch players to survival mode
        List<PlayerInfoData> list = tracker.getPlayerListEntries()
                .stream()
                .filter(entry -> entry.profile.getUUID().version() != 2)
                .filter(entry -> !entry.profile.getUUID().equals(viewerUuid))
                .map(entry -> new PlayerInfoData(entry.profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null))
                .collect(Collectors.toList());

        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);
        packet.getPlayerInfoDataLists().write(0, list);
        ctx.write(packet.getHandle(), ctx.newPromise());

        // check if viewer is spectator
        VanillaTabOverlayTracker.PlayerListEntry viewerEntry = tracker.getPlayerListEntry(viewerUuid);
        this.viewerIsSpectator = viewerEntry != null && viewerEntry.gameMode == EnumWrappers.NativeGameMode.SPECTATOR;

        // add free players
        this.freePlayers.addAll(tracker.getPlayerListEntries()
                .stream()
                .filter(entry -> entry.profile.getUUID().version() != 2)
                .map(entry -> entry.profile.getUUID())
                .collect(Collectors.toList()));

        // start tracking players
        for (UUID uuid : freePlayers) {
            String name = tracker.getPlayerListEntry(uuid).profile.getName();
            teamManager.trackPlayer(ctx, name);
        }


        // mark dirty
        if (!freePlayers.isEmpty()) {
            representation.dirtyFlagSize = true;
            scheduleUpdate();
        }

        // add listener
        tracker.setPlayerListEventListener(this);
    }

    @Override
    @SneakyThrows
    void onDeactivated(ChannelHandlerContext ctx) {
        super.onDeactivated(ctx);

        // remove custom slots
        for (int index = 0; index < 80; index++) {
            if (slotState[index] != SlotState.UNUSED) {
                retireSlot(ctx, index);
            }
        }

        // untrack players
        for (UUID player : freePlayers) {
            VanillaTabOverlayTracker.PlayerListEntry entry = tracker.getPlayerListEntry(player);
            if (entry == null) {
                throw new AssertionError("No player list entry for " + player);
            }
            teamManager.untrackPlayer(ctx, entry.profile.getName());
        }

        // deactivate team manager
        teamManager.deactivate(ctx);

        // remove listener
        tracker.setPlayerListEventListener(null);
    }

    private void checkViewerGameMode() {
        VanillaTabOverlayTracker.PlayerListEntry viewerEntry = tracker.getPlayerListEntry(viewerUuid);
        boolean viewerIsSpectator = viewerEntry != null && viewerEntry.gameMode == EnumWrappers.NativeGameMode.SPECTATOR;

        if (this.viewerIsSpectator != viewerIsSpectator) {
            this.viewerIsSpectator = viewerIsSpectator;
            if (!using80Slots) {
                if (highestUsedSlotIndex >= 0) {
                    dirtySlots.set(highestUsedSlotIndex);
                }
            }

            if (viewerIsSpectator) {
                // mark player slot as dirty
                Integer i = playerUuidToSlotMap.get(viewerUuid);
                if (i != null) {
                    dirtySlots.set(i);
                }
            }
            scheduleUpdate();
        }
    }

    @Override
    public void onPlayerAdded(ChannelHandlerContext ctx, VanillaTabOverlayTracker.PlayerListEntry entry) {
        UUID uuid = entry.profile.getUUID();
        if (uuid.version() != 2 && !freePlayers.contains(uuid) && !playerUuidToSlotMap.containsKey(uuid)) {
            PacketContainer packet = packetHelper.addPlayerListEntry(entry.profile, null, 0, uuid.equals(viewerUuid) ? entry.gameMode : EnumWrappers.NativeGameMode.SURVIVAL);
            ctx.write(packet.getHandle(), ctx.newPromise());
            freePlayers.add(uuid);
            teamManager.trackPlayer(ctx, entry.profile.getName());
        }
        if (uuid.equals(viewerUuid)) {
            checkViewerGameMode();
        }
        int vanillaTabListSize = freePlayers.size() + playerUuidToSlotMap.size();
        if (usedSlotsCount < vanillaTabListSize && !using80Slots) {
            representation.dirtyFlagSize = true;
        }
        scheduleUpdate();
    }

    @Override
    public void onPlayerRemoved(ChannelHandlerContext ctx, VanillaTabOverlayTracker.PlayerListEntry entry) {
        UUID uuid = entry.profile.getUUID();
        String username = entry.profile.getName();
        if (uuid.version() != 2) {
            Integer index = playerUuidToSlotMap.get(uuid);
            if (index != null) {
                retireSlot(ctx, index);
                customSlot(ctx, index);
            }

            teamManager.untrackPlayer(ctx, username);
            freePlayers.remove(uuid);

            PacketContainer packet = packetHelper.removePlayerListEntry(entry.profile.getUUID());
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
        if (uuid.equals(viewerUuid)) {
            checkViewerGameMode();
        }
        int vanillaTabListSize = freePlayers.size() + playerUuidToSlotMap.size();
        if (usedSlotsCount > vanillaTabListSize && canShrink) {
            representation.dirtyFlagSize = true;
            scheduleUpdate();
        }
    }

    @Override
    public void onGameModeUpdate(ChannelHandlerContext ctx, VanillaTabOverlayTracker.PlayerListEntry entry) {
        UUID uuid = entry.profile.getUUID();
        if (uuid.equals(viewerUuid)) {
            val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);
            packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(entry.profile, 0, entry.gameMode, null)));
            ctx.write(packet.getHandle(), ctx.newPromise());

            checkViewerGameMode();
        }
    }

    @Override
    @SneakyThrows
    void networkTick(ChannelHandlerContext ctx) {
        if (dirty) {
            dirty = false;

            if (representation.dirtyFlagSize) {
                representation.dirtyFlagSize = false;
                if (viewerIsSpectator && highestUsedSlotIndex >= 0) {
                    dirtySlots.set(highestUsedSlotIndex);
                }
                updateSize();
                highestUsedSlotIndex = usedSlots.previousSetBit(79);
                this.usedSlotsCount = usedSlots.cardinality();
                using80Slots = this.usedSlotsCount == 80;

                if (!using80Slots && viewerIsSpectator && highestUsedSlotIndex >= 0) {
                    dirtySlots.set(highestUsedSlotIndex);
                }
            }

            dirtySlots.orAndClear(representation.dirtyFlagsUuid);

            if (!dirtySlots.isEmpty() || !freePlayers.isEmpty()) {
                // mark slots as dirty currently being used with the uuid of dirty slots
                for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                    UUID uuid = representation.uuid[index];
                    if (uuid != null) {
                        Integer i = playerUuidToSlotMap.get(uuid);
                        if (i != null) {
                            dirtySlots.set(i);
                        }
                    }
                }

                // pass 1: free players
                for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                    if (usedSlots.get(index)) {
                        if (slotState[index] == SlotState.PLAYER && !Objects.equals(slotUuid[index], representation.uuid[index])) {
                            retireSlot(ctx, index);
                        }
                    } else {
                        if (slotState[index] != SlotState.UNUSED) {
                            retireSlot(ctx, index);
                        }
                    }
                }

                if (!using80Slots && viewerIsSpectator && !viewerUuid.equals(slotUuid[highestUsedSlotIndex])) {
                    if (slotState[highestUsedSlotIndex] != SlotState.UNUSED) {
                        retireSlot(ctx, highestUsedSlotIndex);
                    }
                    if (playerUuidToSlotMap.containsKey(viewerUuid)) {
                        dirtySlots.set(playerUuidToSlotMap.get(viewerUuid));
                        retireSlot(ctx, playerUuidToSlotMap.get(viewerUuid));
                    }
                    playerSlot(ctx, highestUsedSlotIndex, viewerUuid);
                }

                // pass 2: assign players to new slots
                for (int repeat = 1; repeat > 0; repeat--) {
                    for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                        if (usedSlots.get(index) && slotState[index] != SlotState.PLAYER) {
                            UUID uuid = representation.uuid[index];
                            if (uuid != null && freePlayers.contains(uuid) && (!using80Slots || !viewerIsSpectator || !Objects.equals(viewerUuid, uuid))) {
                                retireSlot(ctx, index);
                                playerSlot(ctx, index, uuid);
                            }
                        }
                    }

                    // should not happen too often
                    if (!freePlayers.isEmpty()) {
                        for (int slot = 0; slot < 80; slot++) {
                            UUID uuid;
                            if (slotState[slot] == SlotState.CUSTOM && (uuid = representation.uuid[slot]) != null && freePlayers.contains(uuid)) {
                                dirtySlots.set(slot);
                                repeat = 2;
                            }
                        }
                    }
                }

                // pass 3: distribute remaining 'freePlayers' on the tab list
                int index = 80;
                if (!using80Slots) {
                    for (UUID uuid : new ArrayList<>(freePlayers)) {
                        for (index = usedSlots.previousSetBit(index - 1); index >= 0; index = usedSlots.previousSetBit(index - 1)) {
                            if (slotState[index] != SlotState.PLAYER) {
                                // switch slot to player mode using the player 'uuid'
                                if (slotState[index] == SlotState.CUSTOM) {
                                    // custom -> unused
                                    retireSlot(ctx, index);
                                }
                                playerSlot(ctx, index, uuid);
                                break;
                            }
                        }
                        if (index < 0) {
                            throw new AssertionError("Not enough space on player list.");
                        }
                    }
                }

                // pass 4: switch some slots from unused to custom
                for (index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                    if (usedSlots.get(index)) {
                        if (slotState[index] == SlotState.UNUSED) {
                            // switch slot to custom mode
                            customSlot(ctx, index);
                        }
                    }
                }
            }

            // update icons
            dirtySlots.copyAndClear(representation.dirtyFlagsIcon);
            for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                if (slotState[index] == SlotState.CUSTOM) {
                    customSlot(ctx, index);
                }
            }

            // update text
            dirtySlots.copyAndClear(representation.dirtyFlagsText);
            for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                if (slotState[index] != SlotState.UNUSED) {
                    val packet = packetHelper.updateDisplayName(slotUuid[index], WrappedChatComponent.fromJson(representation.text[index]));
                    ctx.write(packet.getHandle(), ctx.newPromise());
                }
            }

            // update ping
            dirtySlots.copyAndClear(representation.dirtyFlagsPing);
            for (int index = dirtySlots.nextSetBit(0); index >= 0; index = dirtySlots.nextSetBit(index + 1)) {
                if (slotState[index] != SlotState.UNUSED) {
                    val packet = packetHelper.updateLatency(slotUuid[index], representation.ping[index]);
                    ctx.write(packet.getHandle(), ctx.newPromise());
                }
            }

            dirtySlots.clear();
        }
    }

    private void retireSlot(ChannelHandlerContext ctx, int index) {
        switch (slotState[index]) {
            case PLAYER:
                if (playerUuidToSlotMap.get(slotUuid[index]) != index) {
                    throw new AssertionError("playerUuidToSlotMap inconsistent for " + index);
                }
                teamManager.removePlayerFromTeam(ctx, index, slotUsername[index]);
                freePlayers.add(slotUuid[index]);
                break;
            case CUSTOM:
                val packet = packetHelper.removePlayerListEntry(slotUuid[index]);
                ctx.write(packet.getHandle(), ctx.newPromise());
        }
        playerUuidToSlotMap.remove(slotUuid[index], index);
        slotState[index] = SlotState.UNUSED;
        slotUuid[index] = null;
        slotUsername[index] = null;
    }

    private void customSlot(ChannelHandlerContext ctx, int index) {
        representation.dirtyFlagsIcon.clear(index);
        representation.dirtyFlagsText.clear(index);
        representation.dirtyFlagsPing.clear(index);
        Icon icon = representation.icon[index];
        UUID customSlotUuid;
        if (icon.isAlex()) {
            customSlotUuid = Constants.SLOT_UUID_ALEX[index];
        } else {
            customSlotUuid = Constants.SLOT_UUID_STEVE[index];
        }
        String customSlotUsername = Constants.SLOT_USERNAME[index];
        slotUsername[index] = customSlotUsername;
        slotState[index] = SlotState.CUSTOM;
        slotUuid[index] = customSlotUuid;
        PacketContainer packet;
        if (icon.hasTextureProperty()) {
            ProfileProperty iconProperty = icon.getTextureProperty();
            WrappedSignedProperty wrappedProperty = WrappedSignedProperty.fromValues(iconProperty.getName(), iconProperty.getValue(), iconProperty.getSignature());
            packet = packetHelper.addPlayerListEntry(customSlotUuid, customSlotUsername, wrappedProperty, WrappedChatComponent.fromJson(representation.text[index]), representation.ping[index]);
        } else {
            packet = packetHelper.addPlayerListEntry(customSlotUuid, customSlotUsername, WrappedChatComponent.fromJson(representation.text[index]), representation.ping[index]);
        }
        ctx.write(packet.getHandle(), ctx.newPromise());
    }

    private void playerSlot(ChannelHandlerContext ctx, int index, UUID uuid) {
        if (slotState[index] != SlotState.UNUSED) {
            throw new AssertionError("slot " + index + " is not unused");
        }
        if (playerUuidToSlotMap.containsKey(uuid)) {
            throw new AssertionError("player " + uuid + " is already assigned to another slot");
        }
        VanillaTabOverlayTracker.PlayerListEntry playerListEntry = tracker.getPlayerListEntry(uuid);
        if (playerListEntry == null) {
            throw new AssertionError("No player list entry for " + uuid);
        }
        String username = playerListEntry.profile.getName();

        teamManager.addPlayerToTeam(ctx, index, username);
        // Update display name
        PacketContainer packet = packetHelper.updateDisplayName(uuid, WrappedChatComponent.fromJson(representation.text[index]));
        ctx.write(packet.getHandle(), ctx.newPromise());
        // Update ping
        packet = packetHelper.updateLatency(uuid, representation.ping[index]);
        ctx.write(packet.getHandle(), ctx.newPromise());
        // Update slot state
        slotState[index] = SlotState.PLAYER;
        slotUuid[index] = uuid;
        slotUsername[index] = username;
        playerUuidToSlotMap.put(uuid, index);

        freePlayers.remove(uuid);
    }

    private void scheduleUpdate() {
        dirty = true;
        handler.setDirtyFlag();
    }

    class Representation implements TabOverlayHandle.BatchModifiable, TabOverlayHandle {

        final UUID[] uuid;
        final Icon[] icon;
        final String[] text;
        final int[] ping;

        final AtomicInteger batchUpdateRecursionLevel;
        volatile boolean dirtyFlagSize;
        final ConcurrentBitSet dirtyFlagsUuid;
        final ConcurrentBitSet dirtyFlagsIcon;
        final ConcurrentBitSet dirtyFlagsText;
        final ConcurrentBitSet dirtyFlagsPing;

        Representation() {
            this.uuid = new UUID[80];
            this.icon = new Icon[80];
            Arrays.fill(this.icon, Icon.DEFAULT_STEVE);
            this.text = new String[80];
            Arrays.fill(this.text, "{\"text\":\"\"}");
            this.ping = new int[80];
            this.batchUpdateRecursionLevel = new AtomicInteger(0);
            this.dirtyFlagSize = true;
            this.dirtyFlagsUuid = new ConcurrentBitSet(80);
            this.dirtyFlagsIcon = new ConcurrentBitSet(80);
            this.dirtyFlagsText = new ConcurrentBitSet(80);
            this.dirtyFlagsPing = new ConcurrentBitSet(80);
        }

        @Override
        public void beginBatchModification() {
            if (isValid()) {
                if (batchUpdateRecursionLevel.incrementAndGet() < 0) {
                    throw new AssertionError("Recursion level overflow");
                }
            }
        }

        @Override
        public void completeBatchModification() {
            if (isValid()) {
                int level = batchUpdateRecursionLevel.decrementAndGet();
                if (level == 0) {
                    scheduleUpdate();
                } else if (level < 0) {
                    throw new AssertionError("Recursion level underflow");
                }
            }
        }

        void scheduleUpdateIfNotInBatch() {
            if (batchUpdateRecursionLevel.get() == 0) {
                scheduleUpdate();
            }
        }

        void setUuidInternal(int index, @Nullable UUID uuid) {
            if (!Objects.equals(uuid, this.uuid[index])) {
                this.uuid[index] = uuid;
                dirtyFlagsUuid.set(index);
                scheduleUpdateIfNotInBatch();
            }
        }

        void setIconInternal(int index, @Nonnull Icon icon) {
            if (!icon.equals(this.icon[index])) {
                this.icon[index] = icon;
                dirtyFlagsIcon.set(index);
                scheduleUpdateIfNotInBatch();
            }
        }

        void setTextInternal(int index, @Nonnull String text) {
            String jsonText = FastChat.legacyTextToJson(text);
            if (!jsonText.equals(this.text[index])) {
                this.text[index] = jsonText;
                dirtyFlagsText.set(index);
                scheduleUpdateIfNotInBatch();
            }
        }

        void setTextInternal(int index, @Nonnull String text, char alternateColorChar) {
            String jsonText = FastChat.legacyTextToJson(text, alternateColorChar);
            if (!jsonText.equals(this.text[index])) {
                this.text[index] = jsonText;
                dirtyFlagsText.set(index);
                scheduleUpdateIfNotInBatch();
            }
        }

        void setPingInternal(int index, int ping) {
            if (ping != this.ping[index]) {
                this.ping[index] = ping;
                dirtyFlagsPing.set(index);
                scheduleUpdateIfNotInBatch();
            }
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    private enum SlotState {
        UNUSED, CUSTOM, PLAYER
    }
}
