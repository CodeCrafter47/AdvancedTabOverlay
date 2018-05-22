package de.codecrafter47.taboverlay.bukkit.internal;

import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ATOCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AdvancedTabOverlay plugin = JavaPlugin.getPlugin(AdvancedTabOverlay.class);

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage("AdvancedTabOverlay configuration has been reloaded.");
            return true;
        } else if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("info"))) {
            sender.sendMessage("You're using AdvancedTabOverlay v" + plugin.getDescription().getVersion());
            sender.sendMessage("Use /ato reload to reload the configuration");
        }

        return false;
    }
}
