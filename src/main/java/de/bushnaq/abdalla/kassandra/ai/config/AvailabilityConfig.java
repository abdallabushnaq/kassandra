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

package de.bushnaq.abdalla.kassandra.ai.config;

import de.bushnaq.abdalla.kassandra.ai.config.AiFilterConfig.PromptConfig;

/**
 * Configuration for Availability entity AI filtering
 */
public class AvailabilityConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Availability {
                            private float availability;
                            private LocalDate start;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Availability:
                        - Availability represents a users availability to work on projects.
                        - Availability values are floats between 0.0 and 1.0 (e.g., 0.8 = 80% availability)
                        - Due to floating point precision issues, ALWAYS round availability values to 2 decimal places before comparison: Number.parseFloat(entity.getAvailability().toFixed(2))
                        - Convert percentages to decimal values for comparison (e.g., 70% = 0.7, 90% = 0.9)
                        - Start dates indicate when this availability period begins
                        - Support percentage-based queries and date range filtering
                        - Remember: you are filtering Availability entities, so each 'entity' is already an Availability
                        - When queries mention "availability created in 2024" - this means filter by creation year, NOT by checking if entity content contains "availability"
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "80% availability"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) === 0.8;
                        
                        Input: "availability greater than 50%"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) > 0.5;
                        
                        Input: "availability less than 90%"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) < 0.9;
                        
                        Input: "availability greater than or equal to 70%"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) >= 0.7;
                        
                        Input: "availability less than or equal to 40%"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) <= 0.4;
                        
                        Input: "availability between 60% and 80%"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) > 0.6 && Number.parseFloat(entity.getAvailability().toFixed(2)) < 0.8;
                        
                        Input: "full availability"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) === 1.0;
                        
                        Input: "partial availability"
                        Output: return Number.parseFloat(entity.getAvailability().toFixed(2)) > 0.0 && Number.parseFloat(entity.getAvailability().toFixed(2)) < 1.0;
                        
                        Input: "availability starting after January 2025"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2025, 1, 31); return entity.getStart().isAfter(refDate);
                        
                        Input: "availability starting before March 2025"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2025, 3, 1); return entity.getStart().isBefore(refDate);
                        
                        Input: "availability starting in 2025"
                        Output: return entity.getStart().getYear() === 2025;
                        
                        Input: "availability starting this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getStart().getYear() === currentYear;
                        
                        Input: "availability starting this month"
                        Output: const now = Java.type('java.time.LocalDate').now(); return entity.getStart().getYear() === now.getYear() && entity.getStart().getMonth() === now.getMonth();
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "availability created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        """,
                """
                        Examples:
                        Input: "80% availability"
                        Output: return entity.getAvailability() == 0.8f;
                        
                        Input: "availability greater than 50%"
                        Output: return entity.getAvailability() > 0.5f;
                        
                        Input: "availability less than 90%"
                        Output: return entity.getAvailability() < 0.9f;
                        
                        Input: "availability greater than or equal to 70%"
                        Output: return entity.getAvailability() >= 0.7f;
                        
                        Input: "availability less than or equal to 40%"
                        Output: return entity.getAvailability() <= 0.4f;
                        
                        Input: "availability between 60% and 80%"
                        Output: return entity.getAvailability() > 0.6f && entity.getAvailability() < 0.8f;
                        
                        Input: "full availability"
                        Output: return entity.getAvailability() == 1.0f;
                        
                        Input: "partial availability"
                        Output: return entity.getAvailability() > 0.0f && entity.getAvailability() < 1.0f;
                        
                        Input: "availability starting after January 2025"
                        Output: return entity.getStart() != null && entity.getStart().isAfter(LocalDate.of(2025, 1, 31));
                        
                        Input: "availability starting before March 2025"
                        Output: return entity.getStart() != null && entity.getStart().isBefore(LocalDate.of(2025, 3, 1));
                        
                        Input: "availability starting in 2025"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == 2025;
                        
                        Input: "availability starting this year"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == java.time.Year.now().getValue();
                        
                        Input: "availability starting this month"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == java.time.LocalDate.now().getYear() && entity.getStart().getMonth() == java.time.LocalDate.now().getMonth();
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "availability created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        """
        );
    }
}
