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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package de.bushnaq.abdalla.kassandra.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Compares normalized historical values for planning and administrator history.
 */
public final class HistoricalFieldChanges {
    private static final Pattern FRACTIONAL_TIMESTAMP = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\.\\d+(Z|[+-]\\d{2}:\\d{2})$");
    private HistoricalFieldChanges() {
    }

    /**
     * Reports each changed field in stable order.
     *
     * @param before historical values before the revision
     * @param after  historical values at the revision
     * @return field descriptions in old-to-new order
     */
    public static List<String> between(Map<String, ?> before, Map<String, ?> after) {
        Set<String> names = new TreeSet<>(before.keySet());
        names.addAll(after.keySet());
        return names.stream()
                .filter(name -> !Objects.equals(normalize(before.get(name)), normalize(after.get(name))))
                .map(name -> name + ": " + display(name, before.get(name)) + " -> " + display(name, after.get(name)))
                .toList();
    }

    private static String display(String name, Object value) {
        if ("created".equals(name) || "updated".equals(name)) {
            if (value instanceof OffsetDateTime timestamp) {
                return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp.truncatedTo(ChronoUnit.SECONDS));
            }
            if (value instanceof String text) {
                return FRACTIONAL_TIMESTAMP.matcher(text).replaceFirst("$1$2");
            }
        }
        return String.valueOf(normalize(value));
    }

    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new TreeMap<>();
            map.forEach((key, nested) -> {
                if (!"id".equals(key)) {
                    normalized.put(String.valueOf(key), normalize(nested));
                }
            });
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(HistoricalFieldChanges::normalize)
                    .sorted(Comparator.comparing(String::valueOf))
                    .toList();
        }
        return value;
    }
}
