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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.internal.util.BitSet;
import de.codecrafter47.taboverlay.handler.RectangularTabOverlay;
import lombok.val;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

class RectangularOperationModeHandler extends CustomContentOperationModeHandler<RectangularOperationModeHandler.Representation> {

    private static final ImmutableMap<RectangularTabOverlay.Dimension, BitSet> DIMENSION_TO_USED_SLOTS;

    static  {
        // build the dimension to used slots map (for the rectangular tab overlay)
        val builder = ImmutableMap.<RectangularTabOverlay.Dimension, BitSet>builder();
        for (int columns = 1; columns <= 4; columns++) {
            for (int rows = 0; rows <= 20; rows++) {
                if (columns != 1 && rows != 0 && columns * rows <= (columns - 1) * 20)
                    continue;
                BitSet usedSlots = new BitSet(80);
                for (int column = 0; column < columns; column++) {
                    for (int row = 0; row < rows; row++) {
                        usedSlots.set(index(column, row));
                    }
                }
                builder.put(new RectangularTabOverlay.Dimension(columns, rows), usedSlots);
            }
        }
        DIMENSION_TO_USED_SLOTS = builder.build();
    }

    private static int index(int column, int row) {
        return column * 20 + row;
    }

    RectangularOperationModeHandler(SafeTabOverlayHandler handler, VanillaTabOverlayTracker tracker, UUID viewerUuid, PacketHelper packetHelper) {
        super(handler, tracker, viewerUuid, packetHelper);
    }

    @Override
    Representation createRepresentation() {
        return new Representation();
    }

    @Override
    void updateSize() {
        Representation tabOverlay = representation;
        RectangularTabOverlay.Dimension size = tabOverlay.getSize();
        int vanillaTabListSize = freePlayers.size() + playerUuidToSlotMap.size();
        if (size.getSize() < vanillaTabListSize && size.getSize() != 80) {
            for (RectangularTabOverlay.Dimension dimension : tabOverlay.getSupportedSizes()) {
                if (dimension.getColumns() < tabOverlay.getSize().getColumns())
                    continue;
                if (dimension.getRows() < tabOverlay.getSize().getRows())
                    continue;
                if (size.getSize() < vanillaTabListSize && size.getSize() != 80) {
                    size = dimension;
                } else if (size.getSize() > dimension.getSize() && dimension.getSize() > vanillaTabListSize) {
                    size = dimension;
                }
            }
            canShrink = true;
        } else {
            canShrink = false;
        }
        BitSet newUsedSlots = DIMENSION_TO_USED_SLOTS.get(size);
        dirtySlots.orXor(usedSlots, newUsedSlots);
        usedSlots = newUsedSlots;
    }

    class Representation extends CustomContentOperationModeHandler.Representation implements RectangularTabOverlay {
        @Nonnull
        private RectangularTabOverlay.Dimension size;

        private Representation() {
            Optional<RectangularTabOverlay.Dimension> dimensionZero = getSupportedSizes().stream().filter(size -> size.getSize() == 0).findAny();
            if (!dimensionZero.isPresent()) {
                throw new AssertionError();
            }
            this.size = dimensionZero.get();
        }

        @Nonnull
        @Override
        public RectangularTabOverlay.Dimension getSize() {
            return size;
        }

        @Override
        public Collection<RectangularTabOverlay.Dimension> getSupportedSizes() {
            return DIMENSION_TO_USED_SLOTS.keySet();
        }

        @Override
        public void setSize(@Nonnull RectangularTabOverlay.Dimension size) {
            if (!getSupportedSizes().contains(size)) {
                throw new IllegalArgumentException("Unsupported size " + size);
            }
            if (isValid() && !this.size.equals(size)) {
                BitSet oldUsedSlots = DIMENSION_TO_USED_SLOTS.get(this.size);
                BitSet newUsedSlots = DIMENSION_TO_USED_SLOTS.get(size);
                BitSet diff = new BitSet(80);
                diff.orXor(oldUsedSlots, newUsedSlots);
                for (int index = diff.nextSetBit(0); index >= 0; index = diff.nextSetBit(index + 1)) {
                    uuid[index] = null;
                    icon[index] = Icon.DEFAULT_STEVE;
                    text[index] = Constants.EMPTY_JSON_TEXT;
                    ping[index] = 0;
                }
                this.size = size;
                this.dirtyFlagSize = true;
                scheduleUpdateIfNotInBatch();
            }
        }

        @Override
        public void setSlot(int column, int row, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                beginBatchModification();
                try {
                    int index = index(column, row);
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
        public void setSlot(int column, int row, @Nonnull Icon icon, @Nonnull String text, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                beginBatchModification();
                try {
                    int index = index(column, row);
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
        public void setSlot(int column, int row, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                beginBatchModification();
                try {
                    int index = index(column, row);
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
        public void setSlot(int column, int row, @Nullable UUID uuid, @Nonnull Icon icon, @Nonnull String text, char alternateColorChar, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                beginBatchModification();
                try {
                    int index = index(column, row);
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
        public void setUuid(int column, int row, @Nullable UUID uuid) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                setUuidInternal(index(column, row), uuid);
            }
        }

        @Override
        public void setIcon(int column, int row, @Nonnull Icon icon) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                setIconInternal(index(column, row), icon);
            }
        }

        @Override
        public void setText(int column, int row, @Nonnull String text) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                setTextInternal(index(column, row), text);
            }
        }

        @Override
        public void setText(int column, int row, @Nonnull String text, char alternateColorChar) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                setTextInternal(index(column, row), text, alternateColorChar);
            }
        }

        @Override
        public void setPing(int column, int row, int ping) {
            if (isValid()) {
                Preconditions.checkElementIndex(column, size.getColumns(), "column");
                Preconditions.checkElementIndex(row, size.getRows(), "row");
                setPingInternal(index(column, row), ping);
            }
        }
    }
}
