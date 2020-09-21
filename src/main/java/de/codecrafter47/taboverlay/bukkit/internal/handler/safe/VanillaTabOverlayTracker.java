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
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.*;

final class VanillaTabOverlayTracker {

    private PacketHelper packetHelper;

    @Getter
    private WrappedChatComponent header = null;

    @Getter
    private WrappedChatComponent footer = null;

    private Map<String, String> playerToTeamMap = new HashMap<>();

    private Map<String, PacketHelper.TeamProperties> teamProperties = new HashMap<>();

    private Map<String, Set<String>> teamPlayers = new HashMap<>();

    private Map<UUID, PlayerListEntry> playerListEntries = new HashMap<>();

    @Getter
    @Setter
    private TeamEventListener teamEventListener;

    @Getter
    @Setter
    private PlayerListEventListener playerListEventListener;

    VanillaTabOverlayTracker(PacketHelper packetHelper) {
        this.packetHelper = packetHelper;
    }

    void onPacketSending(PacketContainer packet, ChannelHandlerContext ctx) {
        PacketType type = packet.getType();
        if (type == PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER) {
            header = packet.getChatComponents().read(0);
            footer = packet.getChatComponents().read(1);
        } else if (type == PacketType.Play.Server.PLAYER_INFO) {
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            List<PlayerInfoData> data = packet.getPlayerInfoDataLists().read(0);
            switch (action) {
                case ADD_PLAYER:
                    for (PlayerInfoData infoData : data) {
                        PlayerListEntry entry = playerListEntries.remove(infoData.getProfile().getUUID());
                        if (entry != null) {
                            fireOnPlayerRemoved(ctx, entry);
                        }
                        entry = new PlayerListEntry(infoData);
                        playerListEntries.put(infoData.getProfile().getUUID(), entry);
                        fireOnPlayerAdded(ctx, entry);
                    }
                    break;
                case UPDATE_GAME_MODE:
                    for (PlayerInfoData infoData : data) {
                        PlayerListEntry entry = playerListEntries.get(infoData.getProfile().getUUID());
                        if (entry != null) {
                            entry.gameMode = infoData.getGameMode();
                            fireOnGameModeUpdate(ctx, entry);
                        }
                    }
                    break;
                case UPDATE_LATENCY:
                    for (PlayerInfoData infoData : data) {
                        PlayerListEntry entry = playerListEntries.get(infoData.getProfile().getUUID());
                        if (entry != null) {
                            entry.latency = infoData.getLatency();
                        }
                    }
                    break;
                case UPDATE_DISPLAY_NAME:
                    for (PlayerInfoData infoData : data) {
                        PlayerListEntry entry = playerListEntries.get(infoData.getProfile().getUUID());
                        if (entry != null) {
                            entry.displayName = infoData.getDisplayName();
                        }
                    }
                    break;
                case REMOVE_PLAYER:
                    for (PlayerInfoData infoData : data) {
                        PlayerListEntry entry = playerListEntries.remove(infoData.getProfile().getUUID());
                        if (entry != null) {
                            fireOnPlayerRemoved(ctx, entry);
                        }
                    }
                    break;
            }
        }
    }

    void onPacketSent(ChannelHandlerContext ctx, PacketContainer packet) {
        PacketType type = packet.getType();
        if (type == PacketType.Play.Server.SCOREBOARD_TEAM) {
            String teamName = packetHelper.getTeamName(packet);
            switch (packetHelper.getTeamMode(packet)) {
                case CREATE:
                    // remove if already exists
                    Set<String> playerSet = teamPlayers.remove(teamName);
                    if (playerSet != null) {
                        for (String player : playerSet) {
                            if (playerToTeamMap.remove(player, teamName)) {
                                fireOnPlayerRemovedFromTeam(ctx, player, teamName);
                            }
                        }
                    }
                    // add
                    Collection<String> players = packetHelper.getTeamPlayers(packet);
                    PacketHelper.TeamProperties properties = packetHelper.getTeamProperties(packet);
                    this.teamProperties.put(teamName, properties);
                    teamPlayers.put(teamName, new HashSet<>(players));
                    fireOnTeamPropertiesUpdated(ctx, teamName, properties);
                    for (String player : players) {
                        String oldTeam = playerToTeamMap.put(player, teamName);
                        if (oldTeam != null) {
                            fireOnPlayerRemovedFromTeam(ctx, player, oldTeam);
                        }
                        fireOnPlayerAddedToTeam(ctx, player, teamName);
                    }
                    break;
                case REMOVE:
                    this.teamProperties.remove(teamName);
                    playerSet = teamPlayers.remove(teamName);
                    if (playerSet != null) {
                        for (String player : playerSet) {
                            if (playerToTeamMap.remove(player, teamName)) {
                                fireOnPlayerRemovedFromTeam(ctx, player, teamName);
                            }
                        }
                    }
                    break;
                case UPDATE:
                    properties = packetHelper.getTeamProperties(packet);
                    this.teamProperties.put(teamName, properties);
                    fireOnTeamPropertiesUpdated(ctx, teamName, properties);
                    break;
                case ADD_PLAYERS:
                    players = packetHelper.getTeamPlayers(packet);
                    playerSet = this.teamPlayers.get(teamName);
                    if (playerSet != null) {
                        playerSet.addAll(players);
                        for (String player : players) {
                            String oldTeam = playerToTeamMap.put(player, teamName);
                            if (oldTeam != null) {
                                fireOnPlayerRemovedFromTeam(ctx, player, oldTeam);
                            }
                            fireOnPlayerAddedToTeam(ctx, player, teamName);
                        }
                    }
                    break;
                case REMOVE_PLAYERS:
                    players = packetHelper.getTeamPlayers(packet);
                    playerSet = this.teamPlayers.get(teamName);
                    if (playerSet != null) {
                        playerSet.removeAll(players);
                        for (String player : players) {
                            if (playerToTeamMap.remove(player, teamName)) {
                                fireOnPlayerRemovedFromTeam(ctx, player, teamName);
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Nullable
    PacketHelper.TeamProperties getTeamProperties(String teamName) {
        return teamProperties.get(teamName);
    }

    @Nullable
    Set<String> getTeamMembers(String teamName) {
        return teamPlayers.get(teamName);
    }

    @Nullable
    String getTeamForPlayer(String player) {
        return playerToTeamMap.get(player);
    }

    @Nullable
    PlayerListEntry getPlayerListEntry(UUID uuid) {
        return playerListEntries.get(uuid);
    }

    Collection<PlayerListEntry> getPlayerListEntries() {
        return  playerListEntries.values();
    }

    private void fireOnPlayerRemovedFromTeam(ChannelHandlerContext ctx, String player, String team) {
        if (teamEventListener != null) {
            teamEventListener.onPlayerRemovedFromTeam(ctx, player, team);
        }
    }

    private void fireOnPlayerAddedToTeam(ChannelHandlerContext ctx, String player, String team) {
        if (teamEventListener != null) {
            teamEventListener.onPlayerAddedToTeam(ctx, player, team);
        }
    }

    private void fireOnTeamPropertiesUpdated(ChannelHandlerContext ctx, String team, PacketHelper.TeamProperties properties) {
        if (teamEventListener != null) {
            teamEventListener.onTeamPropertiesUpdated(ctx, team, properties);
        }
    }

    private void fireOnPlayerAdded(ChannelHandlerContext ctx, PlayerListEntry entry) {
        if (playerListEventListener != null) {
            playerListEventListener.onPlayerAdded(ctx, entry);
        }
    }

    private void fireOnPlayerRemoved(ChannelHandlerContext ctx, PlayerListEntry entry) {
        if (playerListEventListener != null) {
            playerListEventListener.onPlayerRemoved(ctx, entry);
        }
    }

    private void fireOnGameModeUpdate(ChannelHandlerContext ctx, PlayerListEntry entry) {
        if (playerListEventListener != null) {
            playerListEventListener.onGameModeUpdate(ctx, entry);
        }
    }

    interface TeamEventListener {

        void onPlayerRemovedFromTeam(ChannelHandlerContext ctx, String player, String team);

        void onPlayerAddedToTeam(ChannelHandlerContext ctx, String player, String team);

        void onTeamPropertiesUpdated(ChannelHandlerContext ctx, String team, PacketHelper.TeamProperties properties);
    }

    interface PlayerListEventListener {
        void onPlayerAdded(ChannelHandlerContext ctx, PlayerListEntry entry);

        void onPlayerRemoved(ChannelHandlerContext ctx, PlayerListEntry entry);

        void onGameModeUpdate(ChannelHandlerContext ctx, PlayerListEntry entry);
    }

    static class PlayerListEntry {

        PlayerListEntry(PlayerInfoData data) {
            latency = data.getLatency();
            gameMode = data.getGameMode();
            profile = data.getProfile();
            displayName = data.getDisplayName();
        }

        int latency;
        EnumWrappers.NativeGameMode gameMode;
        WrappedGameProfile profile;
        WrappedChatComponent displayName;
    }
}
