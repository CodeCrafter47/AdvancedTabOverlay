package de.codecrafter47.taboverlay.bukkit;

import com.google.common.collect.ImmutableSet;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.codecrafter47.taboverlay.TabView;
import de.codecrafter47.taboverlay.bukkit.internal.*;
import de.codecrafter47.taboverlay.bukkit.internal.config.MainConfig;
import de.codecrafter47.taboverlay.bukkit.internal.config.PlayersByWorldComponentConfiguration;
import de.codecrafter47.taboverlay.bukkit.internal.handler.safe.SafeTabOverlayHandlerFactory;
import de.codecrafter47.taboverlay.bukkit.internal.placeholders.PAPIAwarePlayerPlaceholderResolver;
import de.codecrafter47.taboverlay.bukkit.internal.placeholders.PlayerPlaceholderResolver;
import de.codecrafter47.taboverlay.bukkit.internal.util.Completer;
import de.codecrafter47.taboverlay.config.ComponentSpec;
import de.codecrafter47.taboverlay.config.ConfigTabOverlayManager;
import de.codecrafter47.taboverlay.config.ErrorHandler;
import de.codecrafter47.taboverlay.config.dsl.CustomPlaceholderConfiguration;
import de.codecrafter47.taboverlay.config.icon.DefaultIconManager;
import de.codecrafter47.taboverlay.config.platform.EventListener;
import de.codecrafter47.taboverlay.config.platform.Platform;
import de.codecrafter47.taboverlay.spectator.SpectatorPassthroughTabOverlayManager;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AdvancedTabOverlay extends JavaPlugin implements Listener {

    private PlayerTabViewManager tabViewManager;
    private TabOverlayHandlerFactory tabOverlayHandlerFactory;
    private ConfigTabOverlayManager configTabOverlayManager;
    private SpectatorPassthroughTabOverlayManager spectatorPassthroughTabOverlayManager;
    private EventListener listener;
    @Getter
    private EventExecutor tabEventQueue;
    @Getter
    private EventExecutorGroup asyncExecutor;
    private PlayerManager playerManager;
    @Getter
    private DataManager dataManager;
    @Getter
    private DefaultIconManager iconManager;
    private Future<Void> softReloadTask;
    private MainConfig config;
    private Yaml yaml;

    @Override
    public void onLoad() {
        // todo init API
        dataManager = new DataManager(this);
    }

    @Override
    @SneakyThrows // todo catch exceptions
    public void onEnable() {
        Metrics metrics = new Metrics(this);
        getCommand("ato").setExecutor(new ATOCommand());
        getCommand("ato").setTabCompleter(Completer.create().any("reload", "info"));
        Executor executor = (task) -> getServer().getScheduler().runTaskAsynchronously(this, task);
        asyncExecutor = new DefaultEventExecutorGroup(4);
        tabEventQueue = new DefaultEventExecutor();
        playerManager = new PlayerManager(this);

        tabViewManager = new PlayerTabViewManager(this, getLogger(), asyncExecutor);

        //tabOverlayHandlerFactory = new AggressiveTabOverlayHandlerFactory(this);
        tabOverlayHandlerFactory = new SafeTabOverlayHandlerFactory();

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

        iconManager = new DefaultIconManager(asyncExecutor, tabEventQueue, getDataFolder().toPath().resolve("icons"), getLogger());
        boolean hasPlaceholderAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

        ConfigTabOverlayManager.Options options = ConfigTabOverlayManager.Options.createBuilderWithDefaults()
                .playerIconDataKey(ATODataKeys.ICON)
                .playerPingDataKey(ATODataKeys.PING)
                .playerInvisibleDataKey(ATODataKeys.HIDDEN)
                .playerCanSeeInvisibleDataKey(MinecraftData.permission("advancedtaboverlay.seehidden"))
                .component(new ComponentSpec("!players_by_world", PlayersByWorldComponentConfiguration.class))
                .build();
        yaml = ConfigTabOverlayManager.constructYamlInstance(options);

        MyPlatform platform = new MyPlatform();
        configTabOverlayManager = new ConfigTabOverlayManager(platform,
                playerManager,
                hasPlaceholderAPI
                        ? new PAPIAwarePlayerPlaceholderResolver()
                        : new PlayerPlaceholderResolver(),
                Collections.emptySet(),
                yaml,
                options,
                getLogger(),
                tabEventQueue,
                iconManager);
        spectatorPassthroughTabOverlayManager = new SpectatorPassthroughTabOverlayManager(platform, tabEventQueue, ATODataKeys.GAMEMODE);

        getServer().getScheduler().scheduleSyncDelayedTask(this, this::onServerFullyLoaded);
        getServer().getPluginManager().registerEvents(this, this);
        dataManager.enable();

        for (Player player : getServer().getOnlinePlayers()) {
            addPlayer(player);
        }
    }

    private void loadMainConfig() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                config = new MainConfig();
                config.needWrite = true;
            } else {
                ErrorHandler.set(new ErrorHandler());
                config = yaml.loadAs(new FileInputStream(configFile), MainConfig.class);
                ErrorHandler errorHandler = ErrorHandler.get();
                ErrorHandler.set(null);
                if (!errorHandler.getEntries().isEmpty()) {
                    getLogger().log(Level.WARNING, errorHandler.formatErrors(configFile.getName()));
                }
            }
            if (config.needWrite) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
                config.write(writer, yaml);
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to load config.yml: " + ex.getMessage(), ex);
            if (config == null) {
                config = new MainConfig();
            }
        }
    }

    @SneakyThrows
    private void onServerFullyLoaded() {
        Path tabLists = getDataFolder().toPath().resolve("tabLists");
        Files.createDirectories(tabLists);
        reload();
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
        TabView tabView = tabViewManager.get(bukkitPlayer);
        tabView.getTabOverlayProviders().activate(tabOverlayHandlerFactory.create(bukkitPlayer));
        tabOverlayHandlerFactory.onCreated(tabView, bukkitPlayer);
        if (listener != null) {
            listener.onTabViewAdded(tabView, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.onPlayerDisconnect(player);
        TabView tabView = tabViewManager.get(player);
        tabViewManager.removeFromPlayer(player);
        if (listener != null) {
            listener.onTabViewRemoved(tabView);
        }
        tabView.deactivate();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        scheduleSoftReload();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        scheduleSoftReload();
    }

    public TabView getTabView(Player player) {
        return tabViewManager.get(player);
    }

    public void reload() {
        loadMainConfig();

        if (config.disableCustomTabListForSpectators) {
            spectatorPassthroughTabOverlayManager.enable();
        } else {
            spectatorPassthroughTabOverlayManager.disable();
        }

        if (config.customPlaceholders != null) {
            val customPlaceholders = new HashMap<String, CustomPlaceholderConfiguration>();
            for (val entry : config.customPlaceholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    customPlaceholders.put(entry.getKey(), entry.getValue());
                }
            }
            configTabOverlayManager.setGlobalCustomPlaceholders(customPlaceholders);
        }

        Path tabLists = getDataFolder().toPath().resolve("tabLists");
        configTabOverlayManager.reloadConfigs(ImmutableSet.of(tabLists));
    }

    private void scheduleSoftReload() {
        if (softReloadTask == null) {
            softReloadTask = getServer().getScheduler().callSyncMethod(this, () -> {
                softReload();
                return null;
            });
        }
    }

    private void softReload() {
        softReloadTask = null;
        dataManager.updateHooks();
        configTabOverlayManager.refreshConfigs();
    }

    private final class MyPlatform implements Platform {

        @Override
        public void addEventListener(EventListener listener) {
            AdvancedTabOverlay.this.listener = listener;
        }
    }
}
