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

package de.codecrafter47.taboverlay.bukkit.internal;

import com.google.common.base.Joiner;
import de.codecrafter47.taboverlay.Icon;
import de.codecrafter47.taboverlay.bukkit.AdvancedTabOverlay;
import de.codecrafter47.taboverlay.bukkit.internal.util.Util;
import io.netty.channel.ChannelHandler;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        } else if (args.length == 1 && args[0].equalsIgnoreCase("pipeline")) {
            Player player = (Player) sender;
            val channel = Util.getChannel(player);
            List<String> pipeline = new ArrayList<>();
            for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
                pipeline.add(entry.getKey());
            }

            player.sendMessage("Pipeline: " + Joiner.on(", ").join(pipeline));
        }

        return false;
    }
}
