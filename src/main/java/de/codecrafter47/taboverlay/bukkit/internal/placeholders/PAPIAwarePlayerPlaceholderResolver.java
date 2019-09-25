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
                        val constructor = create(ATODataKeys.createPlaceholderAPIDataKey("%" + token + "%"));
                        addPlaceholder(token, constructor);
                        return constructor.apply(builder, args);
                    }
                }
            }
        }

        throw new UnknownPlaceholderException();
    }
}
