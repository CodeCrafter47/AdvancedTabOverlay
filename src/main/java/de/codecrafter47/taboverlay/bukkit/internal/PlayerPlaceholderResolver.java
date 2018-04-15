package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.data.api.TypeToken;
import de.codecrafter47.data.bukkit.api.BukkitData;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.config.placeholder.AbstractPlayerPlaceholderResolver;
import de.codecrafter47.taboverlay.config.placeholder.PlayerPlaceholder;
import de.codecrafter47.taboverlay.config.player.Player;

public class PlayerPlaceholderResolver extends AbstractPlayerPlaceholderResolver {
    public PlayerPlaceholderResolver() {
        super();
        addPlaceholder("ping", create(ATODataKeys.PING));
        // todo addPlaceholder("client_version", create(BungeeData.ClientVersion));
        addPlaceholder("world", create(MinecraftData.World));
        addPlaceholder("team", create(MinecraftData.Team));
        addPlaceholder("vault_balance", create(MinecraftData.Economy_Balance));
        addPlaceholder("vault_balance2", create(MinecraftData.Economy_Balance, null, b -> {
            if (b >= 10_000_000) {
                return String.format("%1.0fM", b / 1_000_000);
            } else if (b >= 10_000) {
                return String.format("%1.0fK", b / 1_000);
            } else if (b >= 100) {
                return String.format("%1.0f", b);
            } else {
                return String.format("%1.2f", b);
            }
        }));
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
        addPlaceholder("tab_name", create(BukkitData.PlayerListName, Player::getName, null));
        addPlaceholder("display_name", create(MinecraftData.DisplayName, Player::getName, null));
        // todo addPlaceholder("session_duration_seconds", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) (duration.getSeconds() % 60), null, null));
        // todo addPlaceholder("session_duration_minutes", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) ((duration.getSeconds() % 3600) / 60), null, null));
        // todo addPlaceholder("session_duration_hours", create(TypeToken.INTEGER, BungeeData.BungeeCord_SessionDuration, duration -> (int) (duration.getSeconds() / 3600), null, null));
        addPlaceholder("essentials_afk", create(BukkitData.Essentials_IsAFK, player -> false, null));
        // todo bind to data key addPlaceholder("is_hidden", ofFunction(p -> Boolean.toString(BungeeTabListPlus.isHidden(p))));
        // todo addPlaceholder("gamemode", create(MinecraftData.DATA_KEY_GAMEMODE));
        addPlaceholder("askyblock_island_level", create(BukkitData.ASkyBlock_IslandLevel));
        addPlaceholder("askyblock_island_name", create(BukkitData.ASkyBlock_IslandName));
        addPlaceholder("askyblock_team_leader", create(BukkitData.ASkyBlock_TeamLeader));
        addPlaceholder("permission", this::resolvePermissionPlaceholder);
    }

    private PlayerPlaceholder<Boolean, Boolean> resolvePermissionPlaceholder(PlayerPlaceholder.BindPoint bindPoint, String[] tokens) {
        return new PlayerPlaceholder<>(bindPoint, TypeToken.BOOLEAN, MinecraftData.permission(tokens[0]), null, null, getRepresentationFunction(TypeToken.BOOLEAN, tokens, null));
    }
}
