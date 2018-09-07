package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.taboverlay.config.placeholder.PlayerPlaceholder;
import de.codecrafter47.taboverlay.config.placeholder.UnknownPlaceholderException;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.Arrays;
import java.util.Set;

public class PAPIAwarePlayerPlaceholderResolver extends PlayerPlaceholderResolver {

    @Override
    public PlayerPlaceholder<?, ?> resolve(PlayerPlaceholder.BindPoint bindPoint, String[] tokens) throws UnknownPlaceholderException {
        try {
            return super.resolve(bindPoint, tokens);
        } catch (UnknownPlaceholderException ignored) {

        }

        String token = tokens[0];
        Set<String> prefixes = PlaceholderAPI.getRegisteredPlaceholderPlugins();
        for (String prefix : prefixes) {
            if (token.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
                val constructor = create(ATODataKeys.createPlaceholderAPIDataKey("%" + token + "%"));
                addPlaceholder(token, constructor);
                return constructor.apply(bindPoint, Arrays.copyOfRange(tokens, 1, tokens.length));
            }
        }

        throw new UnknownPlaceholderException();
    }
}
