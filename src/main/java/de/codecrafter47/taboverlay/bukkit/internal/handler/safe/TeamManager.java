package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import com.comphenix.protocol.events.PacketContainer;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import java.util.*;

class TeamManager implements VanillaTabOverlayTracker.TeamEventListener {

    private PacketHelper packetHelper;
    private final VanillaTabOverlayTracker tracker;

    private final Set<String> trackedPlayers = new HashSet<>();
    private final Map<String, Integer> playerToSlotMap = new HashMap<>();

    TeamManager(VanillaTabOverlayTracker tracker, PacketHelper packetHelper) {
        this.tracker = tracker;
        this.packetHelper = packetHelper;
    }

    void activate(ChannelHandlerContext ctx) {
        for (int i = 0; i < 80; i++) {
            val packet = packetHelper.createTeam(Constants.SLOT_TEAM_NAME[i], Collections.singleton(Constants.SLOT_USERNAME[i]));
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
        val packet = packetHelper.createTeam(Constants.OVERFLOW_TEAM_NAME, Collections.emptySet());
        ctx.write(packet.getHandle(), ctx.newPromise());
        tracker.setTeamEventListener(this);
    }

    void deactivate(ChannelHandlerContext ctx) {
        if (!trackedPlayers.isEmpty()) {
            throw new AssertionError("Called deactivate, but trackedPlayers is not empty");
        }
        for (int i = 0; i < 80; i++) {
            val packet = packetHelper.removeTeam(Constants.SLOT_TEAM_NAME[i]);
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
        val packet = packetHelper.removeTeam(Constants.OVERFLOW_TEAM_NAME);
        ctx.write(packet.getHandle(), ctx.newPromise());
        tracker.setTeamEventListener(null);
    }

    void trackPlayer(ChannelHandlerContext ctx, String name) {
        if (trackedPlayers.contains(name)) {
            throw new AssertionError("Already tracking " + name);
        }
        trackedPlayers.add(name);
        String vanillaTeam = tracker.getTeamForPlayer(name);
        if (null == vanillaTeam) {
            addToOverflowTeam(ctx, name);
        }
    }

    private void addToOverflowTeam(ChannelHandlerContext ctx, String name) {
        PacketContainer packet = packetHelper.addPlayerToTeam(Constants.OVERFLOW_TEAM_NAME, name);
        ctx.write(packet.getHandle(), ctx.newPromise());
    }

    void addPlayerToTeam(ChannelHandlerContext ctx, int slot, String name) {
        if (!trackedPlayers.contains(name)) {
            throw new AssertionError("Not tracking " + name);
        }
        if (playerToSlotMap.containsKey(name)) {
            throw new AssertionError("player " + name + " has been assigned to a team already.");
        }
        String vanillaTeam = tracker.getTeamForPlayer(name);
        if (null != vanillaTeam) {
            val packet = packetHelper.removePlayerFromTeam(vanillaTeam, name);
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
        if (vanillaTeam == null) {
            removeFromOverflowTeam(ctx, name);
        }

        PacketContainer packet = packetHelper.addPlayerToTeam(Constants.SLOT_TEAM_NAME[slot], name);
        ctx.write(packet.getHandle(), ctx.newPromise());
        playerToSlotMap.put(name, slot);

        if (null != vanillaTeam) {
            PacketHelper.TeamProperties properties = tracker.getTeamProperties(vanillaTeam);
            if (null != properties) {
                packet = packetHelper.updateTeam(Constants.SLOT_TEAM_NAME[slot], properties);
                ctx.write(packet.getHandle(), ctx.newPromise());
            }
        }
    }

    private void removeFromOverflowTeam(ChannelHandlerContext ctx, String name) {
        PacketContainer packet = packetHelper.removePlayerFromTeam(Constants.OVERFLOW_TEAM_NAME, name);
        ctx.write(packet.getHandle(), ctx.newPromise());
    }

    void removePlayerFromTeam(ChannelHandlerContext ctx, int slot, String name) {
        if (!trackedPlayers.contains(name)) {
            throw new AssertionError("Not tracking " + name);
        }
        if (playerToSlotMap.get(name) != slot) {
            if (playerToSlotMap.containsKey(name)) {
                throw new AssertionError("player " + name + " is assigned to a different team");
            } else {
                throw new AssertionError("player " + name + " is assigned to a no team");
            }
        }

        PacketContainer packet = packetHelper.removePlayerFromTeam(Constants.SLOT_TEAM_NAME[slot], name);
        ctx.write(packet.getHandle(), ctx.newPromise());
        playerToSlotMap.remove(name);

        String vanillaTeam = tracker.getTeamForPlayer(name);
        if (null != vanillaTeam) {
            packet = packetHelper.updateTeamWithDefaults(Constants.SLOT_TEAM_NAME[slot]);
            ctx.write(packet.getHandle(), ctx.newPromise());
            packet = packetHelper.addPlayerToTeam(vanillaTeam, name);
            ctx.write(packet.getHandle(), ctx.newPromise());
        } else {
            addToOverflowTeam(ctx, name);
        }
    }

    void untrackPlayer(ChannelHandlerContext ctx, String name) {
        if (!trackedPlayers.contains(name)) {
            throw new AssertionError("Not tracking " + name);
        }
        if (playerToSlotMap.containsKey(name)) {
            throw new AssertionError("player " + name + " is assigned to a team.");
        }
        String vanillaTeam = tracker.getTeamForPlayer(name);
        if (vanillaTeam == null) {
            removeFromOverflowTeam(ctx, name);
        }
        trackedPlayers.remove(name);
    }

    void onTeamPacket(PacketContainer packet) {
        Collection<String> players = packetHelper.getTeamPlayers(packet);
        if (players != null) {
            int toRemove = 0;
            for (String player : players) {
                if (trackedPlayers.contains(player)) {
                    toRemove++;
                }
            }
            if (toRemove > 0) {
                Set<String> newPlayers = new HashSet<>(players.size() - toRemove);
                for (String player : players) {
                    if (!trackedPlayers.contains(player)) {
                        newPlayers.add(player);
                    }
                }
                packetHelper.setTeamPlayers(packet, newPlayers);
            }
        }
    }

    @Override
    public void onPlayerRemovedFromTeam(ChannelHandlerContext ctx, String player, String team) {
        Integer slot = playerToSlotMap.get(player);
        if (slot != null) {
            val packet = packetHelper.updateTeamWithDefaults(Constants.SLOT_TEAM_NAME[slot]);
            ctx.write(packet.getHandle(), ctx.newPromise());
        } else {
            addToOverflowTeam(ctx, player);
        }
    }

    @Override
    public void onPlayerAddedToTeam(ChannelHandlerContext ctx, String player, String team) {
        Integer slot = playerToSlotMap.get(player);
        if (slot != null) {
            PacketHelper.TeamProperties properties = tracker.getTeamProperties(team);
            if (null != properties) {
                val packet = packetHelper.updateTeam(Constants.SLOT_TEAM_NAME[slot], properties);
                ctx.write(packet.getHandle(), ctx.newPromise());
            }
        } else {
            val packet = packetHelper.addPlayerToTeam(team, player);
            ctx.write(packet.getHandle(), ctx.newPromise());
        }
    }

    @Override
    public void onTeamPropertiesUpdated(ChannelHandlerContext ctx, String team, PacketHelper.TeamProperties properties) {
        Set<String> teamMembers = tracker.getTeamMembers(team);
        if (teamMembers != null) {
            for (String player : teamMembers) {
                Integer slot = playerToSlotMap.get(player);
                if (slot != null) {
                    val packet = packetHelper.updateTeam(Constants.SLOT_TEAM_NAME[slot], properties);
                    ctx.write(packet.getHandle(), ctx.newPromise());
                }
            }
        }
    }
}
