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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Compares normalized historical values for planning and administrator history.
 */
public final class HistoricalFieldChanges {
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
                .map(name -> name + ": " + normalize(before.get(name)) + " -> " + normalize(after.get(name)))
                .toList();
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
