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

package de.bushnaq.abdalla.kassandra.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration holder for Kassandra-related settings.
 * Provides static access to configuration values for use in DTOs.
 */
@Component
@ConfigurationProperties(prefix = "kassandra")
@Data
public class KassandraProperties {

    /**
     * Bound from {@code kassandra.ai.*}.
     */
    private Ai       ai       = new Ai();
    /**
     * Static holder so plain DTOs (e.g. User) can access the value without injection.
     * -- GETTER --
     * Get the number of months to look ahead for holidays.
     *
     * @return the number of months to look ahead for holidays
     */
    @Getter
    private static long holidayLookAheadMonths = 24;
    /**
     * Bound from {@code kassandra.holidays.*}.
     */
    private Holidays holidays = new Holidays();

    /**
     * Copies the bound instance values into static fields after Spring has set them.
     */
    @PostConstruct
    void init() {
        holidayLookAheadMonths = holidays.getLookAheadMonths();
    }

    @Data
    public static class Ai {
        /**
         * Bound from {@code kassandra.ai.filter.model}.
         */
        private String filterModel   = "";
        /**
         * Bound from {@code kassandra.ai.insights.model}.
         */
        private String insightsModel = "";
        /**
         * Bound from {@code kassandra.ai.mcp.model}.
         */
        private String mcpModel      = "";
    }

    @Data
    public static class Holidays {
        /**
         * Bound from {@code kassandra.holidays.look-ahead-months}.
         */
        private long lookAheadMonths = 24;
    }
}
