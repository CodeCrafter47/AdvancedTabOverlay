package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.data.bukkit.AbstractBukkitDataAccess;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PAPIDataAccess extends AbstractBukkitDataAccess<Player> {

    public PAPIDataAccess(Logger logger, Plugin plugin) {
        super(logger, plugin);

        addProvider(ATODataKeys.PAPIPlaceholder, (player, key) -> {
            try {
                return PlaceholderAPI.setPlaceholders(player, key.getParameter());
            } catch (Throwable th) {
                logger.log(Level.WARNING, "Failed to query value for placeholder \"" + key.getParameter() + "\" from PlaceholderAPI", th);
                return null;
            }
        });
    }
}
