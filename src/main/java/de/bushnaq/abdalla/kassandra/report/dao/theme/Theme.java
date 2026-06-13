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
import java.util.HashMap;
import java.util.Map;

public class Theme {
    public final BurndownTheme burndownTheme = new BurndownTheme();
    public final CalendarTheme calendarTheme = new CalendarTheme();
    public final ChartTheme    chartTheme    = new ChartTheme();
    public final GanttTheme    ganttTheme    = new GanttTheme();
    public final ETheme        themeVariance;
    public final XAxesTheme    xAxesTheme    = new XAxesTheme();

    public Theme(ETheme themeVariance) {
        this.themeVariance = themeVariance;
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
        Map<String, Integer> result = new HashMap<>();
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
                        if (value instanceof Color) {
                            Color color = (Color) value;
                            result.put(fieldKey, colorToRgb.apply(color));
                        } else if (value instanceof Color[]) {
                            // Handle array fields
                            Color[] colorArray = (Color[]) value;
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
