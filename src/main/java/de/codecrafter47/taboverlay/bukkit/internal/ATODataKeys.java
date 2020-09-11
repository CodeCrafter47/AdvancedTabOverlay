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

package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.api.TypeToken;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.Icon;

public class ATODataKeys {
    public final static TypeToken<Icon> TYPE_TOKEN_ICON = TypeToken.create();

    public final static DataKey<Icon> ICON = new DataKey<>("ato:icon", MinecraftData.SCOPE_PLAYER, TYPE_TOKEN_ICON);
    public final static DataKey<Integer> PING = new DataKey<>("ato:ping", MinecraftData.SCOPE_PLAYER, TypeToken.INTEGER);
    public final static DataKey<Integer> GAMEMODE = new DataKey<>("ato:gamemode", MinecraftData.SCOPE_PLAYER, TypeToken.INTEGER);
    public final static DataKey<Boolean> HIDDEN = new DataKey<>("ato:hidden", MinecraftData.SCOPE_PLAYER, TypeToken.BOOLEAN);

    public static final DataKey<String> PAPIPlaceholder = new DataKey<>("ato:placeholderAPI", MinecraftData.SCOPE_PLAYER, TypeToken.STRING);

    public static DataKey<String> createPlaceholderAPIDataKey(String placeholder) {
        return PAPIPlaceholder.withParameter(placeholder);
    }
}
