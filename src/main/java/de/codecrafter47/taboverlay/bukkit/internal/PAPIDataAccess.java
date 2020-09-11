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

import de.codecrafter47.data.bukkit.AbstractBukkitDataAccess;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PAPIDataAccess extends AbstractBukkitDataAccess<Player> {

    public PAPIDataAccess(Logger logger, Plugin plugin) {
        super(logger, plugin);

        addProvider(ATODataKeys.PAPIPlaceholder, (player, key) -> {
            try {
                return PlaceholderAPI.setPlaceholders(player, key.getParameter());
            } catch (Throwable th) {
                logger.log(Level.WARNING, "Failed to query value for placeholder \"" + key.getParameter() + "\" from PlaceholderAPI", th);
                return null;
            }
        });
    }
}
