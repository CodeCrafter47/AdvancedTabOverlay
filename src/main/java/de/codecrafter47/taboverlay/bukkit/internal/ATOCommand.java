package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

public class ATOCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AdvancedTabOverlay plugin = JavaPlugin.getPlugin(AdvancedTabOverlay.class);

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage("AdvancedTabOverlay configuration has been reloaded.");
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("print-icons")) {
            Map<String, Icon> iconCache = plugin.getIconManager().getIconCache();
            sender.sendMessage("There are " + iconCache.size() + " cached icons:");
            Object[] keys = iconCache.keySet().toArray();
            Arrays.sort(keys);
            for (Object key : keys) {
                Icon icon = iconCache.get(key);
                sender.sendMessage("" + key + ": {\"" + icon.getTextureProperty().getName() + "\", \"" + icon.getTextureProperty().getValue() + "\", \"" + icon.getTextureProperty().getSignature() + "\"}");
            }
            return true;
        } else if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("info"))) {
            sender.sendMessage("You're using AdvancedTabOverlay v" + plugin.getDescription().getVersion());
            sender.sendMessage("Use /ato reload to reload the configuration");
            return true;
        }

        return false;
    }
}
