package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.google.common.base.Preconditions;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.internal.util.BitSet;
import de.codecrafter47.taboverlay.handler.SimpleTabOverlay;
import lombok.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

class SimpleOperationModeHandler extends CustomContentOperationModeHandler<SimpleOperationModeHandler.Representation> {

    private static final BitSet[] SIZE_TO_USED_SLOTS;

    static  {
        // build the size to used slots map (for the simple tab overlay)
        SIZE_TO_USED_SLOTS = new BitSet[81];
        for (int size = 0; size <= 80; size++) {
            BitSet usedSlots = new BitSet(80);
            usedSlots.set(0, size);
            SIZE_TO_USED_SLOTS[size] = usedSlots;
        }
    }

    SimpleOperationModeHandler(SafeTabOverlayHandler handler, VanillaTabOverlayTracker tracker, UUID viewerUuid, PacketHelper packetHelper) {
        super(handler, tracker, viewerUuid, packetHelper);
    }

    @Override
    Representation createRepresentation() {
        return new Representation();
    }

    @Override
    void updateSize() {
        int newSize = representation.size;
        int vanillaTabListSize = freePlayers.size() + playerUuidToSlotMap.size();
        if (newSize != 80 && newSize < vanillaTabListSize) {
            newSize = Integer.min(vanillaTabListSize, 80);
            canShrink = true;
        } else {
            canShrink = false;
        }
        if (newSize > highestUsedSlotIndex + 1) {
            dirtySlots.set(highestUsedSlotIndex + 1, newSize);
        } else if (newSize <= highestUsedSlotIndex) {
            dirtySlots.set(newSize, highestUsedSlotIndex + 1);
        }
        usedSlots = SIZE_TO_USED_SLOTS[newSize];
    }

    class Representation extends CustomContentOperationModeHandler.Representation implements SimpleTabOverlay {
        int size = 0;

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getMaxSize() {
            return 80;
        }

        @Override
        public void setSize(int size) {
            if (size < 0 || size > 80) {
                throw new IllegalArgumentException("size");
            }
            this.size = size;
            dirtyFlagSize = true;
            scheduleUpdateIfNotInBatch();
        }

        @Override
        public void setSlot(int index, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                beginBatchModification();
                try {
                    setUuidInternal(index, uuid);
                    setIconInternal(index, icon);
                    setTextInternal(index, text);
                    setPingInternal(index, ping);
                } finally {
                    completeBatchModification();
                }
            }
        }

        @Override
        public void setSlot(int index, @Nonnull Icon icon, @Nonnull String text, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                beginBatchModification();
                try {
                    setUuidInternal(index, null);
                    setIconInternal(index, icon);
                    setTextInternal(index, text);
                    setPingInternal(index, ping);
                } finally {
                    completeBatchModification();
                }
            }
        }

        @Override
        public void setSlot(int index, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                beginBatchModification();
                try {
                    setUuidInternal(index, uuid);
                    setIconInternal(index, icon);
                    setTextInternal(index, text, alternateColorChar);
                    setPingInternal(index, ping);
                } finally {
                    completeBatchModification();
                }
            }
        }

        @Override
        public void setSlot(int index, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                beginBatchModification();
                try {
                    setUuidInternal(index, null);
                    setIconInternal(index, icon);
                    setTextInternal(index, text, alternateColorChar);
                    setPingInternal(index, ping);
                } finally {
                    completeBatchModification();
                }
            }
        }

        @Override
        public void setUuid(int index, UUID uuid) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                setUuidInternal(index, uuid);
            }
        }

        @Override
        public void setIcon(int index, @Nonnull @NonNull Icon icon) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                setIconInternal(index, icon);
            }
        }

        @Override
        public void setText(int index, @Nonnull @NonNull String text) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                setTextInternal(index, text);
            }
        }

        @Override
        public void setText(int index, @Nonnull String text, char alternateColorChar) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                setTextInternal(index, text, alternateColorChar);
            }
        }

        @Override
        public void setPing(int index, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(index, size, "index");
                setPingInternal(index, ping);
            }
        }
    }
}
