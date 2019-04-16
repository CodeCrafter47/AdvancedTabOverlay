package de.codecrafter47.taboverlay.bukkit.internal.placeholders;

import de.codecrafter47.taboverlay.bukkit.internal.ATOContextKeys;
import de.codecrafter47.taboverlay.config.context.ContextKeys;
import de.codecrafter47.taboverlay.config.expression.*;
import de.codecrafter47.taboverlay.config.placeholder.Placeholder;
import de.codecrafter47.taboverlay.config.placeholder.PlaceholderResolver;
import de.codecrafter47.taboverlay.config.placeholder.UnknownPlaceholderException;
import de.codecrafter47.taboverlay.config.player.Player;
import de.codecrafter47.taboverlay.config.player.PlayerSet;
import de.codecrafter47.taboverlay.config.template.TemplateCreationContext;
import de.codecrafter47.taboverlay.config.view.AbstractActiveElement;
import de.codecrafter47.taboverlay.config.view.text.TextView;
import de.codecrafter47.taboverlay.config.view.text.TextViewUpdateListener;

import javax.annotation.Nonnull;

// todo allow format
public class WorldPlaceholderResolver implements PlaceholderResolver {
    @Override
    public Placeholder resolve(String[] value, TemplateCreationContext tcc) throws UnknownPlaceholderException {
        if (value.length >= 1 && "world".equalsIgnoreCase(value[0])) {
            if (value.length == 2 && "name".equalsIgnoreCase(value[1])) {
                return new WorldIdPlaceholder();
            } else if (value.length == 2 && "player_count".equalsIgnoreCase(value[1])) {
                return new WorldPlayerCountPlaceholder();
            } else if (value.length == 1) {
                return new WorldIdPlaceholder();
            }
        }
        throw new UnknownPlaceholderException();
    }

    public static class WorldIdPlaceholder implements Placeholder {

        @Override
        public ToStringExpression instantiateWithStringResult() {
            return new ToStringInstance();
        }

        @Override
        public ToDoubleExpression instantiateWithDoubleResult() {
            return Conversions.toDouble(instantiateWithStringResult());
        }

        @Override
        public ToBooleanExpression instantiateWithBooleanResult() {
            return Conversions.toBoolean(instantiateWithStringResult());
        }

        @Override
        public boolean requiresViewerContext() {
            return true; // todo too lazy to check, playing safe
        }

        @Override
        @Nonnull
        public TextView instantiate() {
            return new TextViewInstance();
        }

        private static class AbstractInstance<T> extends AbstractActiveElement<T> {

            @Override
            protected void onActivation() {

            }

            @Override
            protected void onDeactivation() {

            }
        }

        private static class ToStringInstance extends AbstractInstance<ExpressionUpdateListener> implements ToStringExpression {

            @Override
            public String evaluate() {
                return getContext().getCustomObject(ATOContextKeys.WORLD_ID);
            }
        }

        private static class TextViewInstance extends AbstractInstance<TextViewUpdateListener> implements TextView {

            @Override
            public String getText() {
                return getContext().getCustomObject(ATOContextKeys.WORLD_ID);
            }
        }
    }

    private static class WorldPlayerCountPlaceholder implements Placeholder {

        private WorldPlayerCountPlaceholder() {
        }

        @Override
        public ToStringExpression instantiateWithStringResult() {
            return new ToStringInstance();
        }

        @Override
        public ToDoubleExpression instantiateWithDoubleResult() {
            return new ToDoubleInstance();
        }

        @Override
        public ToBooleanExpression instantiateWithBooleanResult() {
            return Conversions.toBoolean(instantiateWithDoubleResult());
        }

        @Override
        public boolean requiresViewerContext() {
            return true; // todo too lazy to check, playing safe
        }

        @Override
        public TextView instantiate() {
            return new TextViewInstance();
        }

        private static abstract class AbstractInstance<T> extends AbstractActiveElement<T> implements PlayerSet.Listener {

            private PlayerSet playerSet;

            private AbstractInstance() {
            }

            @Override
            protected void onActivation() {
                playerSet = getContext().getCustomObject(ATOContextKeys.WORLD_PLAYER_SET);
                playerSet.addListener(this);
            }

            @Override
            protected void onDeactivation() {
                playerSet.removeListener(this);
            }

            protected final int getPlayerCount() {
                return playerSet.getCount();
            }

            @Override
            public void onPlayerAdded(Player player) {
                notifyListener();
            }

            @Override
            public void onPlayerRemoved(Player player) {
                notifyListener();
            }

            protected abstract void notifyListener();
        }

        private static class ToDoubleInstance extends AbstractInstance<ExpressionUpdateListener> implements ToDoubleExpression {

            private ToDoubleInstance() {
                super();
            }

            @Override
            protected void notifyListener() {
                if (hasListener()) {
                    getListener().onExpressionUpdate();
                }
            }

            @Override
            public double evaluate() {
                return getPlayerCount();
            }
        }

        private static class ToStringInstance extends AbstractInstance<ExpressionUpdateListener> implements ToStringExpression {

            private ToStringInstance() {
                super();
            }

            @Override
            protected void notifyListener() {
                if (hasListener()) {
                    getListener().onExpressionUpdate();
                }
            }

            @Override
            public String evaluate() {
                return Integer.toString(getPlayerCount());
            }
        }

        private static class TextViewInstance extends AbstractInstance<TextViewUpdateListener> implements TextView {

            private TextViewInstance() {
                super();
            }

            @Override
            protected void notifyListener() {
                if (hasListener()) {
                    getListener().onTextUpdated();
                }
            }

            @Override
            public String getText() {
                return Integer.toString(getPlayerCount());
            }
        }
    }
}
