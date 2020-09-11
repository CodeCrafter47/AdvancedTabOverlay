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

package de.codecrafter47.taboverlay.bukkit.internal.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Completer implements TabCompleter {
    private List<TabCompleter> completers = new ArrayList<>();

    public static Completer create() {
        return new Completer();
    }

    public Completer player() {
        completers.add(new TabCompletePlayer());
        return this;
    }

    public Completer any(String... args) {
        completers.add(new TabCompleteCollection(Arrays.asList(args)));
        return this;
    }

    public Completer any(Collection args) {
        completers.add(new TabCompleteCollection(args));
        return this;
    }

    public Completer single(String arg) {
        completers.add(new TabCompleteSingle(arg));
        return this;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        int i = args.length;
        if (i <= completers.size()) {
            return completers.get(i - 1).onTabComplete(sender, command, alias, args);
        }
        return null;
    }

    private static class TabCompletePlayer implements TabCompleter {

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, final String[] args) {
            ArrayList<String> list = new ArrayList<>();
            FluentIterable.from(Bukkit.getOnlinePlayers()).filter(new Predicate<Player>() {
                @Override
                public boolean apply(Player player) {
                    return player.getName().toLowerCase().startsWith(args[args.length - 1].toLowerCase());
                }
            }).transform(new Function<Player, String>() {
                @Override
                public String apply(Player player) {
                    return player.getName();
                }
            }).copyInto(list);
            return list;
        }
    }

    private static class TabCompleteCollection implements TabCompleter {
        private final Collection<String> options;

        private TabCompleteCollection(Collection<String> options) {
            this.options = options;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, final String[] args) {
            ArrayList<String> list = new ArrayList<>();
            FluentIterable.from(options).filter(new Predicate<String>() {
                @Override
                public boolean apply(String option) {
                    return option.toLowerCase().startsWith(args[args.length - 1].toLowerCase());
                }
            }).copyInto(list);
            return list;
        }
    }

    private static class TabCompleteSingle implements TabCompleter {
        private final String result;

        private TabCompleteSingle(String result) {
            this.result = result;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return Lists.newArrayList(result);
        }
    }
}
