/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai;

/**
 * Interface for AI-powered filter generators.
 * Implementations convert natural language queries into different types of filters.
 */
public interface AiFilterGenerator {

    /**
     * Parses a natural language search query and generates a filter.
     *
     * @param query      The natural language query from the user
     * @param entityType The type of entity being searched (e.g., "Product", "Version")
     * @return Filter string for filtering objects (format depends on implementation)
     * @throws RuntimeException if generation fails
     */
    String generateFilter(String query, String entityType);

    /**
     * Gets the filter type supported by this generator.
     *
     * @return The filter type (REGEX, JAVASCRIPT, JAVA)
     */
    FilterType getFilterType();

    /**
     * Enum for different filter types
     */
    enum FilterType {
        JAVASCRIPT,
        JAVA
    }
}
