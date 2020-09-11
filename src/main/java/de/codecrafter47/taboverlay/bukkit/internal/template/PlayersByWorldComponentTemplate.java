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

package de.codecrafter47.taboverlay.bukkit.internal.template;

import de.codecrafter47.taboverlay.bukkit.internal.ATOContextKeys;
import de.codecrafter47.taboverlay.config.context.Context;
import de.codecrafter47.taboverlay.config.expression.template.ExpressionTemplate;
import de.codecrafter47.taboverlay.config.player.PlayerSetFactory;
import de.codecrafter47.taboverlay.config.template.PlayerOrderTemplate;
import de.codecrafter47.taboverlay.config.template.PlayerSetTemplate;
import de.codecrafter47.taboverlay.config.template.component.ComponentTemplate;
import de.codecrafter47.taboverlay.config.template.icon.IconTemplate;
import de.codecrafter47.taboverlay.config.template.ping.PingTemplate;
import de.codecrafter47.taboverlay.config.template.text.TextTemplate;
import de.codecrafter47.taboverlay.config.view.components.ComponentView;
import de.codecrafter47.taboverlay.config.view.components.ContainerComponentView;
import de.codecrafter47.taboverlay.config.view.components.PartitionedPlayersView;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
@Builder
public class PlayersByWorldComponentTemplate implements ComponentTemplate {
    private PlayerSetTemplate playerSet;

    private PlayerSetFactory playerSetFactory;

    private PlayerOrderTemplate playerOrder;

    private ComponentTemplate playerComponent;

    @NonNull
    @Nonnull
    private ComponentTemplate morePlayersComponent;

    @Nullable
    private ComponentTemplate worldHeader;

    @Nullable
    private ComponentTemplate worldFooter;

    @Nullable
    private ComponentTemplate worldSeparator;

    private boolean fillSlotsVertical;
    int minSize;
    /* A value of -1 indicates no limit. */
    int maxSize;
    int minSizePerWorld;
    /* A value of -1 indicates no limit. */
    int maxSizePerWorld;
    private int columns;

    TextTemplate defaultText;
    PingTemplate defaultPing;
    IconTemplate defaultIcon;

    ExpressionTemplate partitionFunction;

    @Override
    public LayoutInfo getLayoutInfo() {
        return LayoutInfo.builder()
                .constantSize(false)
                .minSize(0)
                .blockAligned(true)
                .build();
    }

    @Override
    public ComponentView instantiate() {
        return new ContainerComponentView(new PartitionedPlayersView(fillSlotsVertical ? 1 : columns, playerSet, playerComponent, playerComponent.getLayoutInfo().getMinSize(), morePlayersComponent, morePlayersComponent.getLayoutInfo().getMinSize(), playerOrder, defaultText, defaultPing, defaultIcon, partitionFunction, worldHeader, worldFooter, worldSeparator, minSizePerWorld, maxSizePerWorld, (parent, sectionId, playerSet1) -> {
            Context child = parent.clone();
            child.setCustomObject(ATOContextKeys.WORLD_ID, sectionId);
            child.setCustomObject(ATOContextKeys.WORLD_PLAYER_SET, playerSet1);
            return child;
        }),
                fillSlotsVertical, minSize, maxSize, columns, false);
    }
}
