package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.api.TypeToken;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.Icon;

public class ATODataKeys {
    public final static TypeToken<Icon> TYPE_TOKEN_ICON = TypeToken.create();

    public final static DataKey<Icon> ICON = new DataKey<>("ato:icon", MinecraftData.SCOPE_PLAYER, TYPE_TOKEN_ICON);
    public final static DataKey<Integer> PING = new DataKey<>("ato:ping", MinecraftData.SCOPE_PLAYER, TypeToken.INTEGER);
    public final static DataKey<Boolean> HIDDEN = new DataKey<>("ato:hidden", MinecraftData.SCOPE_PLAYER, TypeToken.BOOLEAN);

    public static final DataKey<String> PAPIPlaceholder = new DataKey<>("btlp:placeholderAPI", MinecraftData.SCOPE_PLAYER, TypeToken.STRING);

    public static DataKey<String> createPlaceholderAPIDataKey(String placeholder) {
        return PAPIPlaceholder.withParameter(placeholder);
    }
}
