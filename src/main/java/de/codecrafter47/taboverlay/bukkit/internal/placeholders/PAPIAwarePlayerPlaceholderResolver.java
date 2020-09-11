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

package de.codecrafter47.taboverlay.bukkit.internal.placeholders;

import de.codecrafter47.taboverlay.bukkit.internal.ATODataKeys;
import de.codecrafter47.taboverlay.config.placeholder.PlaceholderArg;
import de.codecrafter47.taboverlay.config.placeholder.PlaceholderBuilder;
import de.codecrafter47.taboverlay.config.placeholder.PlaceholderException;
import de.codecrafter47.taboverlay.config.placeholder.UnknownPlaceholderException;
import de.codecrafter47.taboverlay.config.player.Player;
import de.codecrafter47.taboverlay.config.template.TemplateCreationContext;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class PAPIAwarePlayerPlaceholderResolver extends PlayerPlaceholderResolver {

    @Nonnull
    @Override
    public PlaceholderBuilder<?, ?> resolve(PlaceholderBuilder<Player, ?> builder, List<PlaceholderArg> args, TemplateCreationContext tcc) throws UnknownPlaceholderException, PlaceholderException {
        try {
            return super.resolve(builder, args, tcc);
        } catch (UnknownPlaceholderException ignored) {

        }

        if (args.size() > 0) {
            if (args.get(0) instanceof PlaceholderArg.Text) {
                String token = args.get(0).getText();
                Set<String> prefixes = PlaceholderAPI.getRegisteredPlaceholderPlugins();
                for (String prefix : prefixes) {
                    if (token.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
                        args.remove(0);
                        val resolver = create(ATODataKeys.createPlaceholderAPIDataKey("%" + token + "%"));
                        addPlaceholder(token, resolver);
                        return resolver.resolve(builder, args, tcc);
                    }
                }
            }
        }

        throw new UnknownPlaceholderException();
    }
}
