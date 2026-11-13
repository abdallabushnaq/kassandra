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
import java.util.*;

public class Theme {
    private static final    Color[]             BASE_COLOR_PALETTE       = {
            new Color(200, 16, 46),    // Ruby Red
            new Color(244, 107, 66),   // Coral Orange
            new Color(245, 196, 0),    // Golden Yellow
            new Color(0, 168, 143),    // Teal Green
            new Color(0, 159, 195),    // Ocean Blue
            new Color(96, 52, 154),    // Royal Purple

            new Color(233, 67, 80),    // Rose Red
            new Color(242, 138, 50),   // Tangerine
            new Color(246, 214, 107),  // Butter Yellow
            new Color(114, 198, 174),  // Mint Green
            new Color(69, 183, 204),   // Turquoise
            new Color(161, 138, 203),  // Lavender

            new Color(245, 166, 173),  // Blush Pink
            new Color(244, 196, 157),  // Peach
            new Color(245, 229, 165),  // Vanilla Cream
            new Color(180, 220, 203),  // Seafoam Green
            new Color(163, 214, 228),  // Powder Blue
            new Color(201, 185, 222),  // Lilac

            //too similar to previous row
            new Color(245, 221, 226),  // Pale Rose
            new Color(240, 225, 210),  // Warm Beige
            new Color(241, 235, 221),  // Ivory
            new Color(220, 235, 221),  // Pale Sage
            new Color(209, 231, 237),  // Ice Blue
            new Color(228, 221, 240)   // Pale Lavender
    };
    /**
     * Number of colors available in the shared palette.
     */
    public static final     int                 PALETTE_SIZE             = BASE_COLOR_PALETTE.length;
    private static volatile Color[]             RANDOMIZED_COLOR_PALETTE = BASE_COLOR_PALETTE.clone();
    public final            BurndownTheme       burndownTheme            = new BurndownTheme();
    public final            CalendarTheme       calendarTheme            = new CalendarTheme();
    public final            ChartTheme          chartTheme               = new ChartTheme();
    public final            GanttTheme          ganttTheme               = new GanttTheme();
    /**
     * Theme colors for the sprint overview chart.
     */
    public final            SprintOverviewTheme sprintOverviewTheme      = new SprintOverviewTheme();
    public final            ETheme              themeVariance;
    public final            XAxesTheme          xAxesTheme               = new XAxesTheme();

//    static {
//        Collections.shuffle(Arrays.asList(RANDOMIZED_COLOR_PALETTE));
//    }

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
        Color[] palette = RANDOMIZED_COLOR_PALETTE;
        return palette[Math.floorMod(index, palette.length)];
    }

    /**
     * Shuffles the shared palette deterministically from the supplied seed.
     *
     * @param seed the seed used to reproduce the palette order
     */
    public static void shufflePalette(long seed) {
        Color[] palette = BASE_COLOR_PALETTE.clone();
        Collections.shuffle(Arrays.asList(palette), new Random(seed));
        RANDOMIZED_COLOR_PALETTE = palette;
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
