package de.codecrafter47.taboverlay.bukkit;

import com.google.common.collect.ImmutableSet;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.bukkit.internal.*;
import de.codecrafter47.taboverlay.bukkit.internal.util.Completer;
import de.codecrafter47.taboverlay.config.ConfigTabOverlayManager;
import de.codecrafter47.taboverlay.config.icon.DefaultIconManager;
import de.codecrafter47.taboverlay.config.platform.EventListener;
import de.codecrafter47.taboverlay.config.platform.Platform;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AdvancedTabOverlay extends JavaPlugin implements Listener {

    private final Map<Player, TabView> playerTabViewMap = new IdentityHashMap<>();
    private TabOverlayHandlerFactory tabOverlayHandlerFactory;
    private ConfigTabOverlayManager configTabOverlayManager;
    private EventListener listener;
    @Getter
    private EventExecutor tabEventQueue;
    @Getter
    private EventExecutorGroup asyncExecutor;
    private PlayerManager playerManager;
    @Getter
    private DataManager dataManager;

    @Override
    public void onLoad() {
        // todo init API
        dataManager = new DataManager(this);
    }

    @Override
    @SneakyThrows // todo catch exceptions
    public void onEnable() {
        getCommand("ato").setExecutor(new ATOCommand());
        getCommand("ato").setTabCompleter(Completer.create().any("reload", "info"));
        Executor executor = (task) -> getServer().getScheduler().runTaskAsynchronously(this, task);
        asyncExecutor = new MultithreadEventExecutorGroup(4, executor) {
            @Override
            protected EventExecutor newChild(Executor executor, Object... args) {
                return new DefaultEventExecutor(this, executor, 512, RejectedExecutionHandlers.reject());
            }
        };
        tabEventQueue = new DefaultEventExecutor(null,
                executor,
                128_000, RejectedExecutionHandlers.reject());
        playerManager = new PlayerManager(this);

        tabOverlayHandlerFactory = new DefaultTabOverlayHandlerFactory(this);

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            // copy default config
            try {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(getFile()));

                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (!entry.isDirectory() && entryName.startsWith("default/")) {
                        entryName = entryName.substring(8);
                        try {
                            File targetFile = new File(dataFolder, entryName);
                            targetFile.getParentFile().mkdirs();
                            if (!targetFile.exists()) {
                                Files.copy(zipInputStream, targetFile.toPath());
                                getLogger().info("Extracted " + entryName);
                            }
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, "Failed to extract file " + entryName, ex);
                        }
                    }
                }

                zipInputStream.close();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Error extracting files", ex);
            }
        }

        configTabOverlayManager = new ConfigTabOverlayManager(new MyPlatform(),
                playerManager,
                new PlayerPlaceholderResolver(),
                ConfigTabOverlayManager.Options.createBuilderWithDefaults()
                        .playerIconDataKey(ATODataKeys.ICON)
                        .playerPingDataKey(ATODataKeys.PING)
                        .playerInvisibleDataKey(ATODataKeys.HIDDEN)
                        .playerCanSeeInvisibleDataKey(MinecraftData.permission("advancedtaboverlay.seehidden"))
                        .build(),
                getLogger(),
                tabEventQueue,
                new DefaultIconManager(asyncExecutor, tabEventQueue, getDataFolder().toPath().resolve("icons"), getLogger()));

        Path tabLists = getDataFolder().toPath().resolve("tabLists");
        Files.createDirectories(tabLists);
        configTabOverlayManager.reloadConfigs(ImmutableSet.of(tabLists));
        getServer().getPluginManager().registerEvents(this, this);
        dataManager.enable();

        for (Player player : getServer().getOnlinePlayers()) {
            addPlayer(player);
        }

    }

    @Override
    public void onDisable() {
        asyncExecutor.shutdownGracefully();
        tabEventQueue.shutdownGracefully();

        tabOverlayHandlerFactory.onDisable();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player bukkitPlayer = event.getPlayer();
        addPlayer(bukkitPlayer);
    }

    private void addPlayer(Player bukkitPlayer) {
        de.codecrafter47.taboverlay.config.player.Player player = playerManager.onPlayerJoin(bukkitPlayer);
        TabView tabView = new TabView(tabOverlayHandlerFactory.create(bukkitPlayer), getLogger(), asyncExecutor);
        playerTabViewMap.put(bukkitPlayer, tabView);
        tabOverlayHandlerFactory.onCreated(tabView, bukkitPlayer);
        if (listener != null) {
            listener.onTabViewAdded(tabView, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        playerManager.onPlayerDisconnect(event.getPlayer());
        TabView tabView = playerTabViewMap.remove(event.getPlayer());
        if (listener != null && tabView != null) {
            listener.onTabViewRemoved(tabView);
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        dataManager.updateHooks();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        dataManager.updateHooks();
    }

    public TabView getTabView(Player player) {
        return playerTabViewMap.get(player);
    }

    public void reload() {
        Path tabLists = getDataFolder().toPath().resolve("tabLists");
        configTabOverlayManager.reloadConfigs(ImmutableSet.of(tabLists));
    }

    private final class MyPlatform implements Platform {

        @Override
        public void addEventListener(EventListener listener) {
            AdvancedTabOverlay.this.listener = listener;
        }
    }
}
