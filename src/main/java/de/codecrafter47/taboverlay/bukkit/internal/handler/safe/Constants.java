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

package de.codecrafter47.taboverlay.bukkit.internal.handler.safe;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class Constants {


    static final String EMPTY_JSON_TEXT = "{\"text\":\"\"}";

    static final UUID[] SLOT_UUID_STEVE;
    static final UUID[] SLOT_UUID_ALEX;
    static final String[] SLOT_USERNAME;

    static final String[] SLOT_TEAM_NAME;
    static final String OVERFLOW_TEAM_NAME;

    static {
        // generate random uuids for our custom slots
        SLOT_UUID_ALEX = new UUID[80];
        SLOT_UUID_STEVE = new UUID[80];
        int steve = 0, alex = 0;
        while (steve != 80 || alex != 80) {
            UUID uuid = UUID.randomUUID();
            if ((uuid.hashCode() & 1) == 1) {
                if (alex < 80) {
                    SLOT_UUID_ALEX[alex++] = uuid;
                }
            } else {
                if (steve < 80) {
                    SLOT_UUID_STEVE[steve++] = uuid;
                }
            }
        }

        int unique = ThreadLocalRandom.current().nextInt();
        // generate usernames for custom slots
        SLOT_USERNAME = new String[80];
        String emojis = "\u263a\u2639\u2620\u2763\u2764\u270c\u261d\u270d\u2618\u2615\u2668\u2693\u2708\u231b\u231a\u2600\u2b50\u2601\u2602\u2614\u26a1\u2744\u2603\u2604\u2660\u2665\u2666\u2663\u265f\u260e\u2328\u2709\u270f\u2712\u2702\u2692\u2694\u2699\u2696\u2697\u26b0\u26b1\u267f\u26a0\u2622\u2623\u2640\u2642\u267e\u267b\u269c\u303d\u2733\u2734\u2747\u203c\u2b1c\u2b1b\u25fc\u25fb\u25aa\u25ab\u2049\u26ab\u26aa\u3030\u00a9\u00ae\u2122\u2139\u24c2\u3297\u2716\u2714\u2611\u2695\u2b06\u2197\u27a1\u2198\u2b07\u2199\u3299\u2b05\u2196\u2195\u2194\u21a9\u21aa\u2934\u2935\u269b\u2721\u2638\u262f\u271d\u2626\u262a\u262e\u2648\u2649\u264a\u264b\u264c\u264d\u264e\u264f\u2650\u2651\u2652\u2653\u25b6\u25c0\u23cf";
        for (int i = 0; i < 80; i++) {
            //SLOT_USERNAME[i] = String.format("~ATO%08x %02d", unique, i);
            SLOT_USERNAME[i] = String.format("" + emojis.charAt(i), unique, i);
        }

        // generate team names
        SLOT_TEAM_NAME = new String[80];
        for (int i = 0; i < 80; i++) {
            SLOT_TEAM_NAME[i] = String.format(" ATO%08x %02d", unique, i);
        }

        OVERFLOW_TEAM_NAME = String.format(" ATO%08x ~OV", unique);
    }
}
