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
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import lombok.val;
import org.bukkit.ChatColor;

import java.util.*;

class PacketHelper1_19 implements PacketHelper {

    @Override
    public PacketContainer createTeam(String teamName, Set<String> players) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, 0); // mode: create team

        InternalStructure struct = packet.getOptionalStructures().read(0).get();
        struct.getChatComponents().write(0, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // display
        struct.getChatComponents().write(1, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // prefix
        struct.getChatComponents().write(2, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // suffix
        struct.getStrings().write(0, "always"); // name tag visibility
        struct.getStrings().write(1, "always"); // collision rule
        struct.getEnumModifier(ChatColor.class, 5).write(0, ChatColor.RESET);
        struct.getIntegers().write(0, 1); // friendlyFire
        packet.getOptionalStructures().write(0, Optional.of(struct));

        packet.getSpecificModifier(Collection.class).write(0, players); // players
        return packet;
    }

    @Override
    public PacketContainer removeTeam(String teamName) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, 1); // mode: remove team
        return packet;
    }

    @Override
    public PacketContainer addPlayerToTeam(String teamName, String player) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, 3); // mode: add player
        packet.getSpecificModifier(Collection.class).write(0, Collections.singleton(player));
        return packet;
    }

    @Override
    public PacketContainer removePlayerFromTeam(String teamName, String player) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, 4); // mode: remove player
        packet.getSpecificModifier(Collection.class).write(0, Collections.singleton(player));
        return packet;
    }

    @Override
    public PacketContainer updateTeam(String name, TeamProperties properties) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, name);
        packet.getIntegers().write(0, 2);
        properties.applyTo(packet);
        return packet;
    }

    @Override
    public PacketContainer updateTeamWithDefaults(String name) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, name);
        packet.getIntegers().write(0, 2);

        InternalStructure struct = packet.getOptionalStructures().read(0).get();
        struct.getChatComponents().write(0, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // display
        struct.getChatComponents().write(1, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // prefix
        struct.getChatComponents().write(2, SafeTabOverlayHandler.CHAT_COMPONENT_EMPTY); // suffix
        struct.getStrings().write(0, "always"); // name tag visibility
        struct.getStrings().write(1, "always"); // collision rule
        struct.getEnumModifier(ChatColor.class, 5).write(0, ChatColor.RESET);
        struct.getIntegers().write(0, 1); // friendlyFire
        packet.getOptionalStructures().write(0, Optional.of(struct));

        return packet;
    }

    @Override
    public TeamProperties getTeamProperties(PacketContainer packet) {
        InternalStructure struct = packet.getOptionalStructures().read(0).get();
        val display = struct.getChatComponents().read(0);
        val prefix = struct.getChatComponents().read(1);
        val suffix = struct.getChatComponents().read(2);
        val nameTagVisibility = struct.getStrings().read(0);
        val collisionRule = struct.getStrings().read(1);
        val color = struct.getEnumModifier(ChatColor.class, 5).read(0);
        val friendlyFire = struct.getIntegers().read(0);
        return new TeamProperties() {
            @Override
            public void applyTo(PacketContainer packet) {

                InternalStructure struct = packet.getOptionalStructures().read(0).get();
                struct.getChatComponents().write(0, display);
                struct.getChatComponents().write(1, prefix);
                struct.getChatComponents().write(2, suffix);
                struct.getStrings().write(0, nameTagVisibility);
                struct.getStrings().write(1, collisionRule);
                struct.getEnumModifier(ChatColor.class, 5).write(0, color);
                struct.getIntegers().write(0, friendlyFire);
                packet.getOptionalStructures().write(0, Optional.of(struct));
            }
        };
    }

    @Override
    public TeamMode getTeamMode(PacketContainer packet) {
        Integer mode = packet.getIntegers().read(0);
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
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(entry.profile, latency, gameMode, displayName, ((VanillaTabOverlayTracker.PlayerListEntry119) entry).profileKeyData)));
        return packet;
    }

    @Override
    public PacketContainer removePlayerListEntry(UUID uuid) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), 0, EnumWrappers.NativeGameMode.SURVIVAL, null)));
        return packet;
    }

    @Override
    public PacketContainer updateDisplayName(UUID uuid, WrappedChatComponent displayName) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), 0, EnumWrappers.NativeGameMode.SURVIVAL, displayName)));
        return packet;
    }

    @Override
    public PacketContainer updateLatency(UUID uuid, int latency) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(new WrappedGameProfile(uuid, "dummy"), latency, EnumWrappers.NativeGameMode.SURVIVAL, null)));
        return packet;
    }
}
