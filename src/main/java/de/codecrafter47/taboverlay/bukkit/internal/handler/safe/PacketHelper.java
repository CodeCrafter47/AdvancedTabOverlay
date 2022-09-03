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

package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PacketHelper {
    PacketContainer createTeam(String teamName, Set<String> players);

    PacketContainer removeTeam(String teamName);

    PacketContainer addPlayerToTeam(String teamName, String player);

    PacketContainer removePlayerFromTeam(String teamName, String player);

    PacketContainer updateTeam(String name, TeamProperties properties);

    PacketContainer updateTeamWithDefaults(String name);

    TeamProperties getTeamProperties(PacketContainer packet);

    TeamMode getTeamMode(PacketContainer packet);

    Collection<String> getTeamPlayers(PacketContainer packet);

    void setTeamPlayers(PacketContainer packet, Set<String> players);

    String getTeamName(PacketContainer packet);

    PacketContainer addPlayerListEntry(UUID uuid, String name, WrappedChatComponent displayName, int latency);

    PacketContainer addPlayerListEntry(UUID uuid, String name, WrappedSignedProperty textureProperty, WrappedChatComponent displayName, int latency);

    PacketContainer addPlayerListEntry(VanillaTabOverlayTracker.PlayerListEntry entry, WrappedChatComponent displayName, int latency, EnumWrappers.NativeGameMode gameMode);

    PacketContainer removePlayerListEntry(UUID uuid);

    PacketContainer updateDisplayName(UUID uuid, WrappedChatComponent displayName);

    PacketContainer updateLatency(UUID uuid, int latency);

    public enum TeamMode {
        CREATE, REMOVE, UPDATE, ADD_PLAYERS, REMOVE_PLAYERS
    }

    public interface TeamProperties {

        void applyTo(PacketContainer packet);
    }
}
