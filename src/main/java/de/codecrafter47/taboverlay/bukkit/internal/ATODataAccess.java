package de.codecrafter47.taboverlay.bukkit.internal;

import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import de.codecrafter47.data.bukkit.AbstractBukkitDataAccess;
import de.codecrafter47.data.bukkit.PlayerDataAccess;
import de.codecrafter47.data.bukkit.api.BukkitData;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.ProfileProperty;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.logging.Logger;

public class ATODataAccess extends AbstractBukkitDataAccess<Player> {

    private static FieldAccessor PING;
    private final PlayerDataAccess playerDataAccess;

    public ATODataAccess(Logger logger, Plugin plugin) {
        super(logger, plugin);

        addProvider(ATODataKeys.PING, ATODataAccess::getPlayerPing);
        addProvider(ATODataKeys.ICON, ATODataAccess::getPlayerIcon);
        addProvider(ATODataKeys.HIDDEN, this::isPlayerInvisible);
        playerDataAccess = new PlayerDataAccess(plugin);
    }

    private static Icon getPlayerIcon(Player player) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
        Collection<WrappedSignedProperty> textures = gameProfile.getProperties().get("textures");
        if (textures.isEmpty()) {
            if ((gameProfile.getUUID().hashCode() & 1) == 1) {
                return Icon.DEFAULT_ALEX;
            } else {
                return Icon.DEFAULT_STEVE;
            }
        } else {
            WrappedSignedProperty property = textures.iterator().next();
            return new Icon(new ProfileProperty(property.getName(), property.getValue(), property.getSignature()));
        }
    }

    @SneakyThrows
    private static Integer getPlayerPing(Player player) {
        if (PING == null) {
            PING = Accessors.getFieldAccessor(MinecraftReflection.getEntityPlayerClass(), "ping", false);
        }
        Object nmsPlayer = BukkitUnwrapper.getInstance().unwrapItem(player);
        return (Integer) PING.get(nmsPlayer);
    }

    private Boolean isPlayerInvisible(Player player) {
        if (Boolean.TRUE.equals(playerDataAccess.get(BukkitData.CMI_IsVanished, player)))
            return true;
        if (Boolean.TRUE.equals(playerDataAccess.get(BukkitData.Essentials_IsVanished, player)))
            return true;
        if (Boolean.TRUE.equals(playerDataAccess.get(BukkitData.SuperVanish_IsVanished, player)))
            return true;
        if (Boolean.TRUE.equals(playerDataAccess.get(BukkitData.VanishNoPacket_IsVanished, player)))
            return true;
        return false;
    }
}
