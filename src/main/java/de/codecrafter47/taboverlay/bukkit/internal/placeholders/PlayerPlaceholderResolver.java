package de.codecrafter47.taboverlay.bukkit.internal.placeholders;

import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.api.TypeToken;
import de.codecrafter47.data.bukkit.api.BukkitData;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.bukkit.internal.ATODataKeys;
import de.codecrafter47.taboverlay.config.context.Context;
import de.codecrafter47.taboverlay.config.expression.ExpressionUpdateListener;
import de.codecrafter47.taboverlay.config.expression.ToStringExpression;
import de.codecrafter47.taboverlay.config.expression.template.ExpressionTemplate;
import de.codecrafter47.taboverlay.config.placeholder.*;
import de.codecrafter47.taboverlay.config.player.Player;
import de.codecrafter47.taboverlay.config.template.TemplateCreationContext;
import de.codecrafter47.taboverlay.config.view.AbstractActiveElement;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class PlayerPlaceholderResolver extends AbstractPlayerPlaceholderResolver {
    public PlayerPlaceholderResolver() {
        super();
        addPlaceholder("ping", create(ATODataKeys.PING));
        // todo addPlaceholder("client_version", create(BungeeData.ClientVersion));
        addPlaceholder("world", create(MinecraftData.World));
        addPlaceholder("team", create(MinecraftData.Team));
        addPlaceholder("vault_balance", create(MinecraftData.Economy_Balance));
        addPlaceholder("vault_balance2", create(MinecraftData.Economy_Balance, b -> {
            if (b >= 10_000_000) {
                return String.format("%1.0fM", b / 1_000_000);
            } else if (b >= 10_000) {
                return String.format("%1.0fK", b / 1_000);
            } else if (b >= 100) {
                return String.format("%1.0f", b);
            } else {
                return String.format("%1.2f", b);
            }
        }, TypeToken.STRING));
        addPlaceholder("multiverse_world_alias", create(BukkitData.Multiverse_WorldAlias));
        addPlaceholder("faction_name", create(BukkitData.Factions_FactionName));
        addPlaceholder("faction_member_count", create(BukkitData.Factions_FactionMembers));
        addPlaceholder("faction_online_member_count", create(BukkitData.Factions_OnlineFactionMembers));
        addPlaceholder("faction_at_current_location", create(BukkitData.Factions_FactionsWhere));
        addPlaceholder("faction_power", create(BukkitData.Factions_FactionPower));
        addPlaceholder("faction_player_power", create(BukkitData.Factions_PlayerPower));
        addPlaceholder("faction_rank", create(BukkitData.Factions_FactionsRank));
        addPlaceholder("SimpleClans_ClanName", create(BukkitData.SimpleClans_ClanName));
        addPlaceholder("SimpleClans_ClanMembers", create(BukkitData.SimpleClans_ClanMembers));
        addPlaceholder("SimpleClans_OnlineClanMembers", create(BukkitData.SimpleClans_OnlineClanMembers));
        addPlaceholder("SimpleClans_ClanTag", create(BukkitData.SimpleClans_ClanTag));
        addPlaceholder("SimpleClans_ClanTagLabel", create(BukkitData.SimpleClans_ClanTagLabel));
        addPlaceholder("SimpleClans_ClanColorTag", create(BukkitData.SimpleClans_ClanColorTag));
        addPlaceholder("vault_primary_group", create(MinecraftData.Permissions_PermissionGroup));
        addPlaceholder("vault_prefix", create(MinecraftData.Permissions_Prefix));
        addPlaceholder("vault_suffix", create(MinecraftData.Permissions_Suffix));
        addPlaceholder("vault_primary_group_prefix", create(MinecraftData.Permissions_PrimaryGroupPrefix));
        addPlaceholder("vault_primary_group_weight", create(MinecraftData.Permissions_PermissionGroupWeight));
        addPlaceholder("vault_player_prefix", create(MinecraftData.Permissions_PlayerPrefix));
        addPlaceholder("health", create(MinecraftData.Health));
        addPlaceholder("max_health", create(MinecraftData.MaxHealth));
        addPlaceholder("location_x", create(MinecraftData.PosX));
        addPlaceholder("location_y", create(MinecraftData.PosY));
        addPlaceholder("location_z", create(MinecraftData.PosZ));
        addPlaceholder("xp", create(MinecraftData.XP));
        addPlaceholder("total_xp", create(MinecraftData.TotalXP));
        addPlaceholder("level", create(MinecraftData.Level));
        addPlaceholder("player_points", create(BukkitData.PlayerPoints_Points));
        addPlaceholder("vault_currency", create(MinecraftData.Economy_CurrencyNameSingular));
        addPlaceholder("vault_currency_plural", create(MinecraftData.Economy_CurrencyNamePlural));
        addPlaceholder("tab_name", create(BukkitData.PlayerListName, (player, name) -> name == null ? player.getName() : name, TypeToken.STRING));
        addPlaceholder("display_name", create(MinecraftData.DisplayName, (player, name) -> name == null ? player.getName() : name, TypeToken.STRING));
        // todo addPlaceholder("session_duration_seconds", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) (duration.getSeconds() % 60), null, null));
        // todo addPlaceholder("session_duration_minutes", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) ((duration.getSeconds() % 3600) / 60), null, null));
        // todo addPlaceholder("session_duration_hours", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) (duration.getSeconds() / 3600), null, null));
        addPlaceholder("essentials_afk", create(BukkitData.Essentials_IsAFK, (player, afk) -> afk == null ? false : afk, TypeToken.BOOLEAN));
        addPlaceholder("is_hidden", create(ATODataKeys.HIDDEN));
        // todo addPlaceholder("gamemode", create(MinecraftData.DATA_KEY_GAMEMODE));
        addPlaceholder("askyblock_island_level", create(BukkitData.ASkyBlock_IslandLevel));
        addPlaceholder("askyblock_island_name", create(BukkitData.ASkyBlock_IslandName));
        addPlaceholder("askyblock_team_leader", create(BukkitData.ASkyBlock_TeamLeader));
        addPlaceholder("permission", this::resolvePermissionPlaceholder);
    }

    @SneakyThrows
    @Nonnull
    private PlaceholderBuilder<?, ?> resolvePermissionPlaceholder(PlaceholderBuilder<Player, ?> builder, List<PlaceholderArg> args, TemplateCreationContext tcc) {
        if (args.isEmpty()) {
            throw new PlaceholderException("Use of permission placeholder lacks specification of specific permission");
        }
        ExpressionTemplate permission = args.remove(0).getExpression();
        Function<Context, Player> playerFunction = builder.getContextTransformation();
        return PlaceholderBuilder.create().acquireData(() -> new PermissionDataProvider(permission.instantiateWithStringResult(), playerFunction), TypeToken.BOOLEAN, builder.isRequiresViewerContext() || permission.requiresViewerContext());
    }

    private static class PermissionDataProvider extends AbstractActiveElement<Runnable> implements PlaceholderDataProvider<Context, Boolean>, ExpressionUpdateListener {
        private final ToStringExpression permission;
        private final Function<Context, Player> playerFunction;
        private DataKey<Boolean> permissionDataKey;

        private PermissionDataProvider(ToStringExpression permission, Function<Context, Player> playerFunction) {
            this.permission = permission;
            this.playerFunction = playerFunction;
        }

        @Override
        protected void onActivation() {
            permission.activate(getContext(), this);
            permissionDataKey = MinecraftData.permission(permission.evaluate());
            if (hasListener()) {
                playerFunction.apply(getContext()).addDataChangeListener(permissionDataKey, getListener());
            }
        }

        @Override
        protected void onDeactivation() {
            permission.deactivate();
            if (hasListener()) {
                playerFunction.apply(getContext()).removeDataChangeListener(permissionDataKey, getListener());
            }
        }

        @Override
        public Boolean getData() {
            return playerFunction.apply(getContext()).get(permissionDataKey);
        }

        @Override
        public void onExpressionUpdate() {
            if (hasListener()) {
                playerFunction.apply(getContext()).removeDataChangeListener(permissionDataKey, getListener());
            }
            permissionDataKey = MinecraftData.permission(permission.evaluate());
            if (hasListener()) {
                playerFunction.apply(getContext()).addDataChangeListener(permissionDataKey, getListener());
                getListener().run();
            }
        }
    }
}
