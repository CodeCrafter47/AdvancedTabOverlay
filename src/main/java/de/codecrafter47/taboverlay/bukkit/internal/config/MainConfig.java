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
package de.codecrafter47.taboverlay.bukkit.internal.config;

import com.google.common.collect.ImmutableList;
import de.codecrafter47.taboverlay.config.dsl.customplaceholder.CustomPlaceholderConfiguration;
import de.codecrafter47.taboverlay.config.dsl.yaml.UpdateableConfig;
import de.codecrafter47.taboverlay.config.dsl.yaml.YamlUtil;
import lombok.val;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class MainConfig implements UpdateableConfig {

    public Map<String, CustomPlaceholderConfiguration> customPlaceholders = new HashMap<>();

    public boolean disableCustomTabListForSpectators = true;

    public String timeZone = TimeZone.getDefault().getID();

    public transient boolean needWrite = false;

    @Override
    public void update(MappingNode node) {
        val outdatedConfigOptions = ImmutableList.<String>of();

        for (String option : outdatedConfigOptions) {
            needWrite |= YamlUtil.contains(node, option);
            YamlUtil.remove(node, option);
        }

        val newConfigOptions = ImmutableList.<String>of(
                "customPlaceholders",
                "disableCustomTabListForSpectators",
                "timeZone"
        );

        for (String option : newConfigOptions) {
            needWrite |= !YamlUtil.contains(node, option);
        }
    }

    public void write(Writer writer, Yaml yaml) throws IOException {

        writer.write(yaml.dumpAs(this, Tag.MAP, null));
        writer.close();
    }
}
