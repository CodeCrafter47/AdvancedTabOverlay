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

package de.codecrafter47.taboverlay.bukkit.internal.util;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftFields;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.entity.Player;

@UtilityClass
public class Util {

    @SneakyThrows
    public Channel getChannel(Player player) {
        val manager = MinecraftFields.getNetworkManager(player);
        return ((Channel) FuzzyReflection.fromClass(MinecraftReflection.getNetworkManagerClass()).getFieldByType("channel", Channel.class).get(manager));
    }

    public PlayerInfoData getPlayerInfoData(Player player) {
        return new PlayerInfoData(
                WrappedGameProfile.fromPlayer(player),
                getLatency(player),
                EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                player.getPlayerListName() != null ? WrappedChatComponent.fromText(player.getPlayerListName()) : null);
    }

    @SneakyThrows
    public int getLatency(Player player) {
        return player.getPing();
    }
}
