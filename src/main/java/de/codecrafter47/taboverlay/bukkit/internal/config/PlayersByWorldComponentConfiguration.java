package de.codecrafter47.taboverlay.bukkit.internal.config;

import de.codecrafter47.taboverlay.bukkit.internal.placeholders.WorldPlaceholderResolver;
import de.codecrafter47.taboverlay.bukkit.internal.template.PlayersByWorldComponentTemplate;
import de.codecrafter47.taboverlay.config.dsl.ComponentConfiguration;
import de.codecrafter47.taboverlay.config.dsl.PlayerOrderConfiguration;
import de.codecrafter47.taboverlay.config.dsl.components.BasicComponentConfiguration;
import de.codecrafter47.taboverlay.config.dsl.util.ConfigValidationUtil;
import de.codecrafter47.taboverlay.config.dsl.yaml.MarkedIntegerProperty;
import de.codecrafter47.taboverlay.config.dsl.yaml.MarkedPropertyBase;
import de.codecrafter47.taboverlay.config.dsl.yaml.MarkedStringProperty;
import de.codecrafter47.taboverlay.config.placeholder.OtherCountPlaceholderResolver;
import de.codecrafter47.taboverlay.config.placeholder.PlayerPlaceholderResolver;
import de.codecrafter47.taboverlay.config.template.PlayerOrderTemplate;
import de.codecrafter47.taboverlay.config.template.TemplateCreationContext;
import de.codecrafter47.taboverlay.config.template.component.ComponentTemplate;
import de.codecrafter47.taboverlay.config.template.icon.PlayerIconTemplate;
import de.codecrafter47.taboverlay.config.template.ping.PlayerPingTemplate;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

@Getter
@Setter
public class PlayersByWorldComponentConfiguration extends MarkedPropertyBase implements ComponentConfiguration {

    private PlayerOrderConfiguration playerOrder = PlayerOrderConfiguration.DEFAULT;
    private MarkedStringProperty playerSet;
    private ComponentConfiguration playerComponent = new BasicComponentConfiguration("${player name}");
    @Nullable
    private ComponentConfiguration morePlayersComponent;
    private boolean fillSlotsVertical = false;
    private MarkedIntegerProperty minSize = new MarkedIntegerProperty(0);
    private MarkedIntegerProperty maxSize = new MarkedIntegerProperty(-1);
    private MarkedIntegerProperty minSizePerWorld = new MarkedIntegerProperty(0);
    private MarkedIntegerProperty maxSizePerWorld = new MarkedIntegerProperty(-1);
    @Nullable
    private ComponentConfiguration worldHeader;
    @Nullable
    private ComponentConfiguration worldFooter;
    @Nullable
    private ComponentConfiguration worldSeparator;

    @Override
    public ComponentTemplate toTemplate(TemplateCreationContext tcc) {
        if (ConfigValidationUtil.checkNotNull(tcc, "!players_by_world component", "playerSet", playerSet, getStartMark())) {
            if (!tcc.getPlayerSets().containsKey(playerSet.getValue())) {
                tcc.getErrorHandler().addError("No player set definition available for player set \"" + playerSet.getValue() + "\"", playerSet.getStartMark());
            }
        }

        PlayerOrderTemplate playerOrderTemplate = PlayerOrderConfiguration.DEFAULT.toTemplate(tcc);
        if (ConfigValidationUtil.checkNotNull(tcc, "!players_by_world component", "playerOrder", playerOrder, getStartMark())) {
            playerOrderTemplate = this.playerOrder.toTemplate(tcc);

        }
        if (minSize.getValue() < 0) {
            tcc.getErrorHandler().addError("Failed to configure players component. MinSize is negative", minSize.getStartMark());
        }
        if (maxSize.getValue() != -1 && minSize.getValue() > maxSize.getValue()) {
            tcc.getErrorHandler().addError("Failed to configure players component. MaxSize is lower than minSize", maxSize.getStartMark());
        }

        if (minSizePerWorld.getValue() < 0) {
            tcc.getErrorHandler().addError("Failed to configure players component. MinSizePerWorld is negative", minSizePerWorld.getStartMark());
        }
        if (maxSizePerWorld.getValue() != -1 && minSizePerWorld.getValue() > maxSizePerWorld.getValue()) {
            tcc.getErrorHandler().addError("Failed to configure players component. MaxSizePerWorld is lower than minSizePerWorld", maxSizePerWorld.getStartMark());
        }


        TemplateCreationContext childContextS = tcc.clone();
        if (fillSlotsVertical) {
            childContextS.setColumns(1);
        }
        childContextS.addPlaceholderResolver(new WorldPlaceholderResolver());

        TemplateCreationContext childContextP = childContextS.clone();
        childContextP.setDefaultIcon(new PlayerIconTemplate(PlayerPlaceholderResolver.BindPoint.PLAYER, tcc.getPlayerIconDataKey()));
        childContextP.setDefaultPing(new PlayerPingTemplate(PlayerPlaceholderResolver.BindPoint.PLAYER, tcc.getPlayerPingDataKey()));
        childContextP.setPlayerAvailable(true);

        TemplateCreationContext childContextM = tcc.clone();
        if (fillSlotsVertical) {
            childContextM.setColumns(1);
        }
        childContextM.addPlaceholderResolver(new OtherCountPlaceholderResolver());

        ComponentTemplate playerComponentTemplate = tcc.emptyComponent(); // dummy
        if (ConfigValidationUtil.checkNotNull(tcc, "!players_by_world component", "playerComponent", playerComponent, getStartMark())) {
            playerComponentTemplate = this.playerComponent.toTemplate(childContextP);
            ComponentTemplate.LayoutInfo layoutInfo = playerComponentTemplate.getLayoutInfo();
            if (!layoutInfo.isConstantSize()) {
                tcc.getErrorHandler().addError("Failed to configure !players_by_world component. Attribute playerComponent must not have variable size.", playerComponent.getStartMark());
            }
            if (layoutInfo.isBlockAligned()) {
                tcc.getErrorHandler().addError("Failed to configure !players_by_world component. Attribute playerComponent must not require block alignment.", playerComponent.getStartMark());
            }
        }

        ComponentTemplate morePlayersComponentTemplate;
        if (this.morePlayersComponent != null) {

            morePlayersComponentTemplate = this.morePlayersComponent.toTemplate(childContextM);
            ComponentTemplate.LayoutInfo layoutInfo = morePlayersComponentTemplate.getLayoutInfo();
            if (!layoutInfo.isConstantSize()) {
                tcc.getErrorHandler().addError("Failed to configure !players_by_world component. Attribute playerComponent cannot have variable size.", morePlayersComponent.getStartMark());
            }
            if (layoutInfo.isBlockAligned()) {
                tcc.getErrorHandler().addError("Failed to configure !players_by_world component. Attribute playerComponent must not require block alignment.", morePlayersComponent.getStartMark());
            }
        } else {
            morePlayersComponentTemplate = childContextM.emptyComponent();
        }

        return PlayersByWorldComponentTemplate.builder()
                .playerOrder(playerOrderTemplate)
                .playerSet(tcc.getPlayerSets().get(playerSet.getValue()))
                .playerComponent(playerComponentTemplate)
                .morePlayersComponent(morePlayersComponentTemplate)
                .worldHeader(worldHeader != null ? worldHeader.toTemplate(childContextS) : null)
                .worldFooter(worldFooter != null ? worldFooter.toTemplate(childContextS) : null)
                .worldSeparator(worldSeparator != null ? worldSeparator.toTemplate(tcc) : null)
                .fillSlotsVertical(fillSlotsVertical)
                .minSize(minSize.getValue())
                .maxSize(maxSize.getValue())
                .minSizePerWorld(minSizePerWorld.getValue())
                .maxSizePerWorld(maxSizePerWorld.getValue())
                .columns(tcc.getColumns().orElse(1))
                .defaultIcon(tcc.getDefaultIcon())
                .defaultText(tcc.getDefaultText())
                .defaultPing(tcc.getDefaultPing())
                .partitionFunction(tcc.getExpressionEngine().compile(childContextP, "${player world}", null))
                .build();
    }
}
