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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import lombok.val;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

class PacketHelper1_8 implements PacketHelper {

    @Override
    public PacketContainer createTeam(String teamName, Set<String> players) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(1, 0); // mode: create team
        packet.getStrings().write(1, ""); // display
        packet.getStrings().write(2, ""); // prefix
        packet.getStrings().write(3, ""); // suffix
        packet.getStrings().write(4, "always"); // name tag visibility
        packet.getIntegers().write(0, -1);
        packet.getIntegers().write(2, 1); // friendlyFire
        packet.getSpecificModifier(Collection.class).write(0, players); // players
        return packet;
    }

    @Override
    public PacketContainer removeTeam(String teamName) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(1, 1); // mode: remove team
        return packet;
    }

    @Override
    public PacketContainer addPlayerToTeam(String teamName, String player) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(1, 3); // mode: add player
        packet.getSpecificModifier(Collection.class).write(0, Collections.singleton(player));
        return packet;
    }

    @Override
    public PacketContainer removePlayerFromTeam(String teamName, String player) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(1, 4); // mode: remove player
        packet.getSpecificModifier(Collection.class).write(0, Collections.singleton(player));
        return packet;
    }

    @Override
    public PacketContainer updateTeam(String name, TeamProperties properties) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, name);
        packet.getIntegers().write(1, 2);
        properties.applyTo(packet);
        return packet;
    }

    @Override
    public PacketContainer updateTeamWithDefaults(String name) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, name);
        packet.getIntegers().write(1, 2);
        packet.getStrings().write(1, ""); // display
        packet.getStrings().write(2, ""); // prefix
        packet.getStrings().write(3, ""); // suffix
        packet.getStrings().write(4, "always"); // name tag visibility
        packet.getIntegers().write(0, -1);
        packet.getIntegers().write(2, 1); // friendlyFire
        return packet;
    }

    @Override
    public TeamProperties getTeamProperties(PacketContainer packet) {
        val display = packet.getStrings().read(1);
        val prefix = packet.getStrings().read(2);
        val suffix = packet.getStrings().read(3);
        val nameTagVisibility = packet.getStrings().read(4);
        val color = packet.getIntegers().read(0);
        val friendlyFire = packet.getIntegers().read(2);
        return new TeamProperties() {
            @Override
            public void applyTo(PacketContainer packet) {
                packet.getStrings().write(1, display);
                packet.getStrings().write(2, prefix);
                packet.getStrings().write(3, suffix);
                packet.getStrings().write(4, nameTagVisibility);
                packet.getIntegers().write(0, color);
                packet.getIntegers().write(2, friendlyFire);
            }
        };
    }

    @Override
    public TeamMode getTeamMode(PacketContainer packet) {
        Integer mode = packet.getIntegers().read(1);
        switch (mode) {
            case 0:
                return TeamMode.CREATE;
            case 1:
                return TeamMode.REMOVE;
            case 2:
                return TeamMode.UPDATE;
            case 3:
                return TeamMode.ADD_PLAYERS;
            case 4:
                return TeamMode.REMOVE_PLAYERS;
            default:
                throw new AssertionError("Illegal team mode " + mode);
        }
    }

    @Override
    public Collection<String> getTeamPlayers(PacketContainer packet) {
        return (Collection<String>) packet.getSpecificModifier(Collection.class).read(0);
    }

    @Override
    public void setTeamPlayers(PacketContainer packet, Set<String> players) {
        packet.getSpecificModifier(Collection.class).write(0, players);
    }

    @Override
    public String getTeamName(PacketContainer packet) {
        return packet.getStrings().read(0);
    }

    @Override
    public PacketContainer addPlayerListEntry(UUID uuid, String name, WrappedChatComponent displayName, int latency) {
        WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, name);
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(gameProfile, latency, EnumWrappers.NativeGameMode.SURVIVAL, displayName)));
        return packet;
    }

    @Override
    public PacketContainer addPlayerListEntry(UUID uuid, String name, WrappedSignedProperty textureProperty, WrappedChatComponent displayName, int latency) {
        WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, name);
        gameProfile.getProperties().put(textureProperty.getName(), textureProperty);
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(gameProfile, latency, EnumWrappers.NativeGameMode.SURVIVAL, displayName)));
        return packet;
    }

    @Override
    public PacketContainer addPlayerListEntry(VanillaTabOverlayTracker.PlayerListEntry entry, WrappedChatComponent displayName, int latency, EnumWrappers.NativeGameMode gameMode) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(entry.profile, latency, gameMode, displayName)));
        return packet;
    }

    @Override
    public PacketContainer removePlayerListEntry(UUID uuid) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), 0, EnumWrappers.NativeGameMode.NOT_SET, null)));
        return packet;
    }

    @Override
    public PacketContainer updateDisplayName(UUID uuid, WrappedChatComponent displayName) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), 0, EnumWrappers.NativeGameMode.NOT_SET, displayName)));
        return packet;
    }

    @Override
    public PacketContainer updateLatency(UUID uuid, int latency) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), latency, EnumWrappers.NativeGameMode.NOT_SET, null)));
        return packet;
    }
}
