/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.report.dao.theme;

import de.bushnaq.abdalla.kassandra.report.dao.ETheme;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Theme {
    //    /** Kelly's 19 chromatic colors, shared by light and dark themes. */
//    public static final Color[] KELLY_COLORS = {
//            new Color(0xebce2b), // yellow
//            new Color(0x702c8c), // purple
//            new Color(0xdb6917), // orange
//            new Color(0x96cde6), // light blue
//            new Color(0xba1c30), // red
//            new Color(0xc0bd7f), // buff
//            new Color(0x7f7e80), // grey
//            new Color(0x5fa641), // green
//            new Color(0xd485b2), // purple pink
//            new Color(0xdf8461), // yellow pink
//            new Color(0x463397), // violet
//            new Color(0xe1a11a), // orange yellow
//            new Color(0x91218c), // purple red
//            new Color(0xe8e948), // green yellow
//            new Color(0x7e1510), // red brown
//            new Color(0x92ae31), // yellow green
//            new Color(0x6f340d), // yellow brown
//            new Color(0xd32b1e), // red orange
//            new Color(0x2b3514)  // olive green
//    };
    // Pure, solid Okabe-Ito palette (No transparency adjustments)
//    private static final Color[] OKABE_ITO_PALETTE = {
//            new Color(0x000000), // Black
//            new Color(0xE69F00), // Orange
//            new Color(0x56B4E9), // Sky Blue
//            new Color(0x009E73), // Bluish Green
//            new Color(0xF0E442), // Yellow
//            new Color(0x0072B2), // Blue
//            new Color(0xD55E00), // Vermillion
//            new Color(0xCC79A7)  // Reddish Purple
//    };
    // Line 1: Top Row (Full Saturation)
//    public static final Color[] LINE_1_SATURATED = {
//            new Color(0xBE002A), // Crimson Red
//            new Color(0xE75A24), // Vivid Orange
//            new Color(0xF9BE00), // Marigold Yellow
//            new Color(0x00A572), // Emerald Green
//            new Color(0x0095D9), // Cerulean Blue
//            new Color(0x632FA9)  // Deep Violet
//    };

    // Line 2: Second Row (Soft Tints)
//    public static final Color[] LINE_2_TINTS = {
//            new Color(0xEE5D6C), // Coral Pink
//            new Color(0xF68D4E), // Light Apricot
//            new Color(0xFCD768), // Soft Maize Yellow
//            new Color(0x59C398), // Mint Green
//            new Color(0x4EC1E7), // Sky Blue
//            new Color(0x977CBD)  // Muted Lavender
//    };

    //    private static final Color[] BASE_COLOR_PALETTE = {
//            new Color(0xBE002A), // Crimson Red
//            new Color(0xE75A24), // Vivid Orange
//            new Color(0xF9BE00), // Marigold Yellow
//            new Color(0x00A572), // Emerald Green
//            new Color(0x0095D9), // Cerulean Blue
//            new Color(0x632FA9),  // Deep Violet
//            new Color(0xEE5D6C), // Coral Pink
//            new Color(0xF68D4E), // Light Apricot
//            new Color(0xFCD768), // Soft Maize Yellow
//            new Color(0x59C398), // Mint Green
//            new Color(0x4EC1E7), // Sky Blue
//            new Color(0x977CBD)  // Muted Lavender
//    };
    private static final Color[] BASE_COLOR_PALETTE       = {
            // Red
            new Color(0xC1002D), // Deep Crimson
            new Color(0xE7374B), // Vivid Red
            new Color(0xF99B9D), // Soft Coral
            new Color(0xF9DADE), // Pale Pink

            // Orange
            new Color(0xFF6941), // Vivid Orange
            new Color(0xFF8B2C), // Bright Orange
            new Color(0xFFC397), // Light Apricot
            new Color(0xFAE8D3), // Cream

            // Yellow
            new Color(0xFFBC00), // Marigold Yellow
            new Color(0xFFD350), // Golden Yellow
            new Color(0xFFE7A9), // Soft Maize
            new Color(0xFAF3DD), // Pale Butter

            // Green
            new Color(0x00A484), // Emerald Green
            new Color(0x5BC3A5), // Mint Green
            new Color(0xB1DDCC), // Soft Sage
            new Color(0xE0EFDE), // Pale Mint

            // Blue
            new Color(0x0099BE), // Deep Cyan
            new Color(0x26B9CF), // Turquoise Blue
            new Color(0x9BD6E1), // Sky Blue
            new Color(0xD8EAF0), // Pale Blue

            // Purple
            new Color(0x673097), // Deep Violet
            new Color(0x9775C0), // Muted Lavender
            new Color(0xC7B1D8), // Soft Lilac
            new Color(0xE8DEF1)  // Pale Lavender
    };
    /**
     * Number of colors available in the shared palette.
     */
    public static final int PALETTE_SIZE = BASE_COLOR_PALETTE.length;
    private static final Color[] RANDOMIZED_COLOR_PALETTE = BASE_COLOR_PALETTE.clone();
    public final BurndownTheme burndownTheme = new BurndownTheme();
    public final CalendarTheme calendarTheme = new CalendarTheme();
    public final ChartTheme    chartTheme    = new ChartTheme();
    public final GanttTheme    ganttTheme    = new GanttTheme();
    public final ETheme        themeVariance;
    public final XAxesTheme    xAxesTheme    = new XAxesTheme();

    static {
        Collections.shuffle(Arrays.asList(RANDOMIZED_COLOR_PALETTE));
    }

    public Theme(ETheme themeVariance) {
        this.themeVariance = themeVariance;
    }

    /**
     * Returns a color from the shared palette, wrapping the index when it exceeds the palette size.
     *
     * @param index the palette index
     * @return the color at the wrapped index
     */
    public static Color getPaletteColor(int index) {
        return RANDOMIZED_COLOR_PALETTE[Math.floorMod(index, PALETTE_SIZE)];
    }

    /**
     * Extracts all color fields from Theme and its nested theme objects using reflection.
     * Returns a map with keys in the format "themeName.fieldName" (e.g., "xAxesTheme.dayOfMonthBgColor").
     * Color values are converted to 0xRRGGBB integers (no alpha).
     * Array elements like monthBgColors are represented as "fieldName.index" (e.g., "monthBgColors.0").
     *
     * @return Map with theme color keys and values as RGB integers
     */
    public Map<String, Integer> toMap() {
        Map<String, Integer>                        result     = new HashMap<>();
        java.util.function.Function<Color, Integer> colorToRgb = c -> c == null ? null : ((c.getRed() & 0xff) << 16) | ((c.getGreen() & 0xff) << 8) | (c.getBlue() & 0xff);

        // Use reflection to iterate through all public fields of this Theme instance
        for (Field field : this.getClass().getFields()) {
            if (!java.lang.reflect.Modifier.isPublic(field.getModifiers())) continue;

            try {
                Object themeObj = field.get(this);
                if (themeObj == null) continue;

                String themeName = field.getName();

                // Iterate through all public fields of the nested theme object
                for (Field innerField : themeObj.getClass().getFields()) {
                    if (!java.lang.reflect.Modifier.isPublic(innerField.getModifiers())) continue;

                    String fieldKey = themeName + "." + innerField.getName();

                    try {
                        Object value = innerField.get(themeObj);
                        if (value instanceof Color color) {
                            result.put(fieldKey, colorToRgb.apply(color));
                        } else if (value instanceof Color[] colorArray) {
                            // Handle array fields
                            for (int i = 0; i < colorArray.length; i++) {
                                result.put(fieldKey + "." + i, colorToRgb.apply(colorArray[i]));
                            }
                        }
                    } catch (IllegalAccessException e) {
                        // Skip fields that cannot be accessed
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }

        return result;
    }

}
