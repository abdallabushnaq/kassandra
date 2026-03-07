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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.availability;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for Availability entity AI filtering
 */
public class AvailabilityFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class AvailabilityFilterDto {
                            private int           availabilityPercent; // whole-number percentage 0–100, e.g. 80 means 80%
                            private LocalDate     start;               // never null
                            private OffsetDateTime created;            // never null
                            private OffsetDateTime updated;            // never null
                        }
                        """,
                """
                        Special considerations for Availability:
                        - Availability represents a user's availability to work on projects.
                        - availabilityPercent is an integer from 0 to 100 (e.g. 80 means 80%). There are NO floating-point values.
                        - Use direct integer comparison: entity.getAvailabilityPercent() === 80
                        - Convert percentage queries directly to integers (e.g. "70%" → 70, "90%" → 90)
                        - Start dates indicate when this availability period begins
                        - Support percentage-based queries and date range filtering
                        - Remember: you are filtering AvailabilityFilterDto entities, so each 'entity' is already an AvailabilityFilterDto
                        - When queries mention "availability created in 2024" - this means filter by creation year, NOT by checking if entity content contains "availability"
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "80% availability"
                        Output: return entity.getAvailabilityPercent() === 80;
                        
                        Input: "availability greater than 50%"
                        Output: return entity.getAvailabilityPercent() > 50;
                        
                        Input: "availability less than 90%"
                        Output: return entity.getAvailabilityPercent() < 90;
                        
                        Input: "availability greater than or equal to 70%"
                        Output: return entity.getAvailabilityPercent() >= 70;
                        
                        Input: "availability less than or equal to 40%"
                        Output: return entity.getAvailabilityPercent() <= 40;
                        
                        Input: "availability between 60% and 80% inclusive"
                        Output: return entity.getAvailabilityPercent() >= 60 && entity.getAvailabilityPercent() <= 80;
                        
                        Input: "100% availability"
                        Output: return entity.getAvailabilityPercent() === 100;
                        
                        Input: "0% availability"
                        Output: return entity.getAvailabilityPercent() === 0;
                        
                        Input: "partial availability"
                        Output: return entity.getAvailabilityPercent() > 0 && entity.getAvailabilityPercent() < 100;
                        
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
                        Output: return entity.getAvailabilityPercent() == 80;
                        
                        Input: "availability greater than 50%"
                        Output: return entity.getAvailabilityPercent() > 50;
                        
                        Input: "availability less than 90%"
                        Output: return entity.getAvailabilityPercent() < 90;
                        
                        Input: "availability greater than or equal to 70%"
                        Output: return entity.getAvailabilityPercent() >= 70;
                        
                        Input: "availability less than or equal to 40%"
                        Output: return entity.getAvailabilityPercent() <= 40;
                        
                        Input: "availability between 60% and 80% inclusive"
                        Output: return entity.getAvailabilityPercent() >= 60 && entity.getAvailabilityPercent() <= 80;
                        
                        Input: "100% availability"
                        Output: return entity.getAvailabilityPercent() == 100;
                        
                        Input: "0% availability"
                        Output: return entity.getAvailabilityPercent() == 0;
                        
                        Input: "partial availability"
                        Output: return entity.getAvailabilityPercent() > 0 && entity.getAvailabilityPercent() < 100;
                        
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

