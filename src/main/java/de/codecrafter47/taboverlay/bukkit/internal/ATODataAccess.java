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
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Function;
import java.util.logging.Logger;

public class ATODataAccess extends AbstractBukkitDataAccess<Player> {

    private static Function<Player, Integer> PING;
    private final PlayerDataAccess playerDataAccess;

    public ATODataAccess(Logger logger, Plugin plugin) {
        super(logger, plugin);

        addProvider(ATODataKeys.PING, ATODataAccess::getPlayerPing);
        addProvider(ATODataKeys.GAMEMODE, ATODataAccess::getPlayerGamemode);
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
            try {
                val acessor = Accessors.getFieldAccessor(MinecraftReflection.getEntityPlayerClass(), "ping", false);
                PING = player1 -> {
                    Object nmsPlayer = BukkitUnwrapper.getInstance().unwrapItem(player1);
                    return (Integer) acessor.get(nmsPlayer);
                };
            } catch (IllegalArgumentException ignored) {
                val method = Player.class.getMethod("getPing");
                PING = player1 -> {
                    try {
                        return (Integer) method.invoke(player1);
                    } catch (IllegalAccessException | InvocationTargetException ignored2) {
                        return -1;
                    }
                };
            }
        }
        return PING.apply(player);
    }

    private static Integer getPlayerGamemode(Player player) {
        return player.getGameMode().getValue();
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
