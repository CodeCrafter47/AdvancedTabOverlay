package de.codecrafter47.taboverlay.bukkit.internal.placeholders;

import de.codecrafter47.data.api.TypeToken;
import de.codecrafter47.taboverlay.bukkit.internal.ATOContextKeys;
import de.codecrafter47.taboverlay.config.context.Context;
import de.codecrafter47.taboverlay.config.placeholder.*;
import de.codecrafter47.taboverlay.config.player.Player;
import de.codecrafter47.taboverlay.config.player.PlayerSet;
import de.codecrafter47.taboverlay.config.template.TemplateCreationContext;
import de.codecrafter47.taboverlay.config.view.AbstractActiveElement;

import javax.annotation.Nonnull;
import java.util.List;

public class WorldPlaceholderResolver implements PlaceholderResolver<Context> {

    @Nonnull
    @Override
    public PlaceholderBuilder<?, ?> resolve(PlaceholderBuilder<Context, ?> builder, List<PlaceholderArg> args, TemplateCreationContext tcc) throws UnknownPlaceholderException, PlaceholderException {
        if (args.size() >= 1 && "world".equalsIgnoreCase(args.get(0).getText())) {
            args.remove(0);
            if (args.size() == 1 && "name".equalsIgnoreCase(args.get(0).getText())) {
                args.remove(0);
                return builder.acquireData(WorldIdPlaceholder::new, TypeToken.STRING);
            } else if (args.size() == 1 && "player_count".equalsIgnoreCase(args.get(0).getText())) {
                args.remove(0);
                return builder.acquireData(WorldPlayerCountPlaceholder::new, TypeToken.INTEGER);
            } else if (args.size() == 0) {
                return builder.acquireData(WorldIdPlaceholder::new, TypeToken.STRING);
            }
            throw new PlaceholderException("Unknown world placeholder");
        }
        throw new UnknownPlaceholderException();
    }

    public static class WorldIdPlaceholder extends AbstractActiveElement<Runnable> implements PlaceholderDataProvider<Context, String> {

        @Override
        public String getData() {
            return getContext().getCustomObject(ATOContextKeys.WORLD_ID);
        }

        @Override
        protected void onActivation() {

        }

        @Override
        protected void onDeactivation() {

        }
    }

    private static class WorldPlayerCountPlaceholder extends AbstractActiveElement<Runnable> implements PlaceholderDataProvider<Context, Integer>, PlayerSet.Listener {

        private PlayerSet playerSet;

        @Override
        protected void onActivation() {
            playerSet = getContext().getCustomObject(ATOContextKeys.WORLD_PLAYER_SET);
            playerSet.addListener(this);
        }

        @Override
        public Integer getData() {
            return playerSet.getCount();
        }

        @Override
        protected void onDeactivation() {
            playerSet.removeListener(this);
        }

        @Override
        public void onPlayerAdded(Player player) {
            if (hasListener()) {
                getListener().run();
            }
        }

        @Override
        public void onPlayerRemoved(Player player) {
            if (hasListener()) {
                getListener().run();
            }
        }
    }
}
