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
 * Configuration for OffDay entity AI filtering
 */
public class OffDayConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class OffDay extends AbstractDateRange {
                            private OffDayType type; //never null, enum: VACATION, SICK, TRIP, HOLIDAY
                            private LocalDate firstDay;//never null
                            private LocalDate lastDay;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for OffDays:
                        - Type values: VACATION, SICK, TRIP, HOLIDAY (enum OffDayType)
                        - Date ranges with firstDay and lastDay in LocalDate format
                        - Support type-based filtering and date range queries
                        - Consider duration calculations and overlap queries for scheduling
                        - Remember: you are filtering OffDay entities, so each 'entity' is already an OffDay
                        - When queries mention "off days created in 2024" - this means filter by creation year, NOT by checking if entity content contains "off days"
                        - Use only getter methods like entity.getType(), entity.getFirstDay(), entity.getLastDay(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "vacation"
                        Output: const OffDayType = Java.type('de.bushnaq.abdalla.kassandra.dto.OffDayType'); return entity.getType() === OffDayType.VACATION;
                        
                        Input: "sick days"
                        Output: const OffDayType = Java.type(de.bushnaq.abdalla.kassandra.dto.OffDayType'); return entity.getType() === OffDayType.SICK;
                        
                        Input: "holidays"
                        Output: const OffDayType = Java.type('de.bushnaq.abdalla.kassandra.dto.OffDayType'); return entity.getType() === OffDayType.HOLIDAY;
                        
                        Input: "trips"
                        Output: const OffDayType = Java.type('de.bushnaq.abdalla.kassandra.dto.OffDayType'); return entity.getType() === OffDayType.TRIP;
                        
                        Input: "off days in January 2025"
                        Output: return entity.getFirstDay().getYear() === 2025 && entity.getFirstDay().getMonthValue() === 1;
                        
                        Input: "off days starting after February 2025"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2025, 2, 28); return entity.getFirstDay().isAfter(refDate);
                        
                        Input: "off days ending before March 2025"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2025, 3, 1); return entity.getLastDay().isBefore(refDate);
                        
                        Input: "off days starting in 2025"
                        Output: return entity.getFirstDay().getYear() === 2025;
                        
                        Input: "off days ending in 2025"
                        Output: return entity.getLastDay().getYear() === 2025;
                        
                        Input: "long vacations"
                        Output: const OffDayType = Java.type('de.bushnaq.abdalla.kassandra.dto.OffDayType'); const ChronoUnit = Java.type('java.time.temporal.ChronoUnit'); return entity.getType() === OffDayType.VACATION && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) >= 7;
                        
                        Input: "short trips"
                        Output: const OffDayType = Java.type('de.bushnaq.abdalla.kassandra.dto.OffDayType'); const ChronoUnit = Java.type('java.time.temporal.ChronoUnit'); return entity.getType() === OffDayType.TRIP && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) <= 3;
                        
                        Input: "off days this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getFirstDay().getYear() === currentYear;
                        
                        Input: "off days this month"
                        Output: const now = Java.type('java.time.LocalDate').now(); return entity.getFirstDay().getYear() === now.getYear() && entity.getFirstDay().getMonth() === now.getMonth();
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "off days created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "weekend off days"
                        Output: const DayOfWeek = Java.type('java.time.DayOfWeek'); return entity.getFirstDay().getDayOfWeek() === DayOfWeek.SATURDAY || entity.getFirstDay().getDayOfWeek() === DayOfWeek.SUNDAY;
                        
                        Input: "multi-day off periods"
                        Output: return !entity.getFirstDay().equals(entity.getLastDay());""",
                """
                        Examples:
                        Input: "vacation"
                        Output: return entity.getType() == OffDayType.VACATION;
                        
                        Input: "sick days"
                        Output: return entity.getType() == OffDayType.SICK;
                        
                        Input: "holidays"
                        Output: return entity.getType() == OffDayType.HOLIDAY;
                        
                        Input: "trips"
                        Output: return entity.getType() == OffDayType.TRIP;
                        
                        Input: "off days in January 2025"
                        Output: return entity.getFirstDay().getYear() == 2025 && entity.getFirstDay().getMonthValue() == 1;
                        
                        Input: "off days starting after February 2025"
                        Output: return entity.getFirstDay().isAfter(LocalDate.of(2025, 2, 28));
                        
                        Input: "off days ending before March 2025"
                        Output: return entity.getLastDay().isBefore(LocalDate.of(2025, 3, 1));
                        
                        Input: "off days starting in 2025"
                        Output: return entity.getFirstDay().getYear() == 2025;
                        
                        Input: "off days ending in 2025"
                        Output: return entity.getLastDay().getYear() == 2025;
                        
                        Input: "long vacations"
                        Output: return entity.getType() == OffDayType.VACATION && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) >= 7;
                        
                        Input: "short trips"
                        Output: return entity.getType() == OffDayType.TRIP && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) <= 3;
                        
                        Input: "off days this year"
                        Output: return entity.getFirstDay().getYear() == java.time.Year.now().getValue();
                        
                        Input: "off days this month"
                        Output: return entity.getFirstDay().getYear() == LocalDate.now().getYear() && entity.getFirstDay().getMonth() == LocalDate.now().getMonth();
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "off days created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "weekend off days"
                        Output: return entity.getFirstDay().getDayOfWeek() == DayOfWeek.SATURDAY || entity.getFirstDay().getDayOfWeek() == DayOfWeek.SUNDAY;
                        
                        Input: "multi-day off periods"
                        Output: return !entity.getFirstDay().equals(entity.getLastDay());"""
        );
    }
}
