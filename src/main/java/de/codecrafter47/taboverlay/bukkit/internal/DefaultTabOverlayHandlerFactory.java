package de.codecrafter47.taboverlay.bukkit.internal;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.bukkit.TabOverlayHandlerFactory;
import de.codecrafter47.taboverlay.bukkit.internal.util.UnorderedPair;
import de.codecrafter47.taboverlay.bukkit.internal.util.Util;
import de.codecrafter47.taboverlay.handler.TabOverlayHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultTabOverlayHandlerFactory implements TabOverlayHandlerFactory, Listener {

    private final Map<UnorderedPair<Player>, Double> lastDistance = new HashMap<>();
    private final AdvancedTabOverlay plugin;
    private final Map<Player, DefaultTabOverlayHandler> tabOverlayHandlerMap = new HashMap<>();

    public DefaultTabOverlayHandlerFactory(AdvancedTabOverlay plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::removePlayers, 2, 2);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updatePlayerTracker, 5, 5);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMoved(PlayerMoveEvent event) {
        val tabListHandler = tabOverlayHandlerMap.get(event.getPlayer());
        if (tabListHandler != null) {
            tabListHandler.active = true;
            if (tabListHandler.handler != null) {
                tabListHandler.handler.update();
            }
        }
    }

    private void updatePlayerTracker() {
        // remove players not connected
        lastDistance.keySet().removeIf(it -> !it.a.isOnline() || !it.b.isOnline());

        // update the others
        val playerList = ImmutableList.copyOf(Bukkit.getOnlinePlayers());
        for (int i = 1; i < playerList.size() - 1; i++)
            for (int j = 0; j < i - 1; j++) {
                val a = playerList.get(i);
                val b = playerList.get(j);
                val key = new UnorderedPair<Player>(a, b);
                if (a.getWorld() != b.getWorld()) {
                    lastDistance.put(key, Double.POSITIVE_INFINITY);
                } else {
                    double distance = a.getLocation().distance(b.getLocation());
                    double lastD = lastDistance.getOrDefault(key, Double.POSITIVE_INFINITY);
                    if (distance < 100) {
                        if (distance < lastD && lastD - distance > 20) {
                            if (a.canSee(b)) {
                                a.hidePlayer(b);
                                a.showPlayer(b);
                            }
                            if (b.canSee(a)) {
                                b.hidePlayer(a);
                                b.showPlayer(a);
                            }
                            lastDistance.put(key, distance);
                        }
                    } else {
                        lastDistance.put(key, Double.POSITIVE_INFINITY);
                    }
                }
            }
    }

    private void removePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            val playerTabOverlayHandler = tabOverlayHandlerMap.get(player);
            if (playerTabOverlayHandler != null) {
                playerTabOverlayHandler.tick();
            }
        }
    }

    @Override
    public TabOverlayHandler create(Player player) {
        val channel = Util.getChannel(player);
        val tabOverlayHandler = new DefaultTabOverlayHandler(player);
        val channelHandler = new MyChannelHandler(tabOverlayHandler);
        channel.pipeline().addBefore("packet_handler", "AdvancedTabOverlay-0", channelHandler);
        tabOverlayHandlerMap.put(player, tabOverlayHandler);
        return tabOverlayHandler;
    }

    @Override
    public void onCreated(TabView tabView, Player player) {
        val playerTabOverlayHandler = tabOverlayHandlerMap.get(player);
        playerTabOverlayHandler.handler = new PassthroughPriorFirstMoveHandler(playerTabOverlayHandler, tabView);
        tabView.getTabOverlayProviders().addProvider(playerTabOverlayHandler.handler);
    }

    @RequiredArgsConstructor
    private static class MyChannelHandler extends ChannelOutboundHandlerAdapter {
        private final DefaultTabOverlayHandler tablistHandler;

        private static final Set<PacketType> interceptedPacketTypes = ImmutableSet.of(PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.NAMED_ENTITY_SPAWN, PacketType.Play.Server.RESPAWN, PacketType.Play.Server.POSITION, PacketType.Play.Server.SPAWN_POSITION);

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            val packet = PacketContainer.fromPacket(msg);
            if (interceptedPacketTypes.contains(packet.getType())) {
                if (tablistHandler.onPacketSending(ctx, packet)) {
                    super.write(ctx, packet.getHandle(), promise);
                }
            } else {
                super.write(ctx, msg, promise);
            }
        }
    }
}
