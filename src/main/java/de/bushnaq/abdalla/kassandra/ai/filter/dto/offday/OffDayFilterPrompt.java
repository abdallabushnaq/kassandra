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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.offday;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for OffDay entity AI filtering.
 */
public class OffDayFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class OffDayFilterDto {
                            private String    type;     // never null; one of: VACATION, SICK, TRIP, HOLIDAY
                            private LocalDate firstDay; // never null – start of the off-day period
                            private LocalDate lastDay;  // never null – end of the off-day period (equal to firstDay for single-day entries)
                            private OffsetDateTime created; // never null
                            private OffsetDateTime updated; // never null
                        }
                        """,
                """
                        Special considerations for OffDays:
                        - type is a plain String with one of four values: "VACATION", "SICK", "TRIP", "HOLIDAY"
                        - Compare type with a string literal: entity.getType() === 'VACATION'  (no Java.type() needed)
                        - firstDay and lastDay are LocalDate values; use Java.type('java.time.LocalDate') for date constants
                        - Duration in days = ChronoUnit.DAYS.between(firstDay, lastDay) — inclusive of both ends add 1
                        - Remember: you are filtering OffDayFilterDto entities, so each 'entity' is an OffDayFilterDto
                        - When queries mention "off days created in 2025" - this means filter by creation year
                        - Use only getter methods: entity.getType(), entity.getFirstDay(), entity.getLastDay(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "vacation"
                        Output: return entity.getType() === 'VACATION';
                        
                        Input: "sick days"
                        Output: return entity.getType() === 'SICK';
                        
                        Input: "holidays"
                        Output: return entity.getType() === 'HOLIDAY';
                        
                        Input: "trips"
                        Output: return entity.getType() === 'TRIP';
                        
                        Input: "off days starting in January 2025"
                        Output: return entity.getFirstDay().getYear() === 2025 && entity.getFirstDay().getMonthValue() === 1;
                        
                        Input: "off days starting after February 28 2024"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2024, 2, 28); return entity.getFirstDay().isAfter(refDate);
                        
                        Input: "off days ending before March 1 2024"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2024, 3, 1); return entity.getLastDay().isBefore(refDate);
                        
                        Input: "off days starting in 2025"
                        Output: return entity.getFirstDay().getYear() === 2025;
                        
                        Input: "off days ending in 2025"
                        Output: return entity.getLastDay().getYear() === 2025;
                        
                        Input: "vacations longer than 7 days"
                        Output: const ChronoUnit = Java.type('java.time.temporal.ChronoUnit'); return entity.getType() === 'VACATION' && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) > 7;
                        
                        Input: "off days lasting more than 5 days"
                        Output: const ChronoUnit = Java.type('java.time.temporal.ChronoUnit'); return ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) > 5;
                        
                        Input: "single-day off periods"
                        Output: return entity.getFirstDay().equals(entity.getLastDay());
                        
                        Input: "multi-day off periods"
                        Output: return !entity.getFirstDay().equals(entity.getLastDay());
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "off days created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        """,
                """
                        Examples:
                        Input: "vacation"
                        Output: return entity.getType().equals("VACATION");
                        
                        Input: "sick days"
                        Output: return entity.getType().equals("SICK");
                        
                        Input: "holidays"
                        Output: return entity.getType().equals("HOLIDAY");
                        
                        Input: "trips"
                        Output: return entity.getType().equals("TRIP");
                        
                        Input: "off days starting in January 2025"
                        Output: return entity.getFirstDay().getYear() == 2025 && entity.getFirstDay().getMonthValue() == 1;
                        
                        Input: "off days starting after February 28 2024"
                        Output: return entity.getFirstDay().isAfter(LocalDate.of(2024, 2, 28));
                        
                        Input: "off days ending before March 1 2024"
                        Output: return entity.getLastDay().isBefore(LocalDate.of(2024, 3, 1));
                        
                        Input: "off days starting in 2025"
                        Output: return entity.getFirstDay().getYear() == 2025;
                        
                        Input: "off days ending in 2025"
                        Output: return entity.getLastDay().getYear() == 2025;
                        
                        Input: "vacations longer than 7 days"
                        Output: return entity.getType().equals("VACATION") && java.time.temporal.ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) > 7;
                        
                        Input: "off days lasting more than 5 days"
                        Output: return java.time.temporal.ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) > 5;
                        
                        Input: "single-day off periods"
                        Output: return entity.getFirstDay().equals(entity.getLastDay());
                        
                        Input: "multi-day off periods"
                        Output: return !entity.getFirstDay().equals(entity.getLastDay());
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "off days created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        """
        );
    }
}

