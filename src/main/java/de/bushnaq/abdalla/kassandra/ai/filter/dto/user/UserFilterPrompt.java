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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.user;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for User entity AI filtering.
 */
public class UserFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class UserFilterDto {
                            private String    name;            // never null
                            private String    email;           // never null
                            private LocalDate firstWorkingDay; // never null – hire date / contract start
                            private LocalDate lastWorkingDay;  // null means still employed; non-null means terminated
                            private LocalDate now;             // never null – injected current date for tenure calculations
                            private OffsetDateTime created;    // never null
                            private OffsetDateTime updated;    // never null
                        }
                        """,
                """
                        Special considerations for Users:
                        - User names contain first and last names (e.g., "John Doe", "Jane Smith")
                        - Email addresses follow firstname.lastname@domain.com pattern
                        - Active (still-employed) users have lastWorkingDay === null
                        - Former employees have a non-null lastWorkingDay
                        - firstWorkingDay is the hire/contract-start date; lastWorkingDay is the termination date
                        - For tenure calculations use entity.getNow() — it holds the injected current date
                        - Remember: you are filtering UserFilterDto entities, so each 'entity' is a UserFilterDto
                        - When queries mention "users created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "users"
                        - Terms like "users", "employees", or similar generic terms refer to the entity type, not name content
                        - Use only getter methods: entity.getName(), entity.getEmail(), entity.getFirstWorkingDay(), entity.getLastWorkingDay(), entity.getNow(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "John Doe"
                        Output: return entity.getName().toLowerCase().includes('john doe');
                        
                        Input: "name contains Smith"
                        Output: return entity.getName().toLowerCase().includes('smith');
                        
                        Input: "email contains alice"
                        Output: return entity.getEmail().toLowerCase().includes('alice');
                        
                        Input: "email contains @company.com"
                        Output: return entity.getEmail().toLowerCase().includes('@company.com');
                        
                        Input: "active users"
                        Output: return entity.getLastWorkingDay() === null;
                        
                        Input: "former employees"
                        Output: return entity.getLastWorkingDay() !== null;
                        
                        Input: "lastWorkingDay is not null"
                        Output: return entity.getLastWorkingDay() !== null;
                        
                        Input: "firstWorkingDay in 2018"
                        Output: return entity.getFirstWorkingDay().getYear() === 2018;
                        
                        Input: "users starting after December 31 2020"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2020, 12, 31); return entity.getFirstWorkingDay().isAfter(refDate);
                        
                        Input: "users starting before January 1 2020"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2020, 1, 1); return entity.getFirstWorkingDay().isBefore(refDate);
                        
                        Input: "users starting in 2024"
                        Output: return entity.getFirstWorkingDay().getYear() === 2024;
                        
                        Input: "users starting in June, July or August"
                        Output: const m = entity.getFirstWorkingDay().getMonthValue(); return m === 6 || m === 7 || m === 8;
                        
                        Input: "employees hired before January 1 2022"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2022, 1, 1); return entity.getFirstWorkingDay().isBefore(refDate);
                        
                        Input: "employees hired more than 3 years ago"
                        Output: return entity.getFirstWorkingDay().isBefore(entity.getNow().minusYears(3));
                        
                        Input: "employees started within last 6 months"
                        Output: return entity.getFirstWorkingDay().isAfter(entity.getNow().minusMonths(6));
                        
                        Input: "users ending employment in 2024"
                        Output: return entity.getLastWorkingDay() !== null && entity.getLastWorkingDay().getYear() === 2024;
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "users created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        """,
                """
                        Examples:
                        Input: "John Doe"
                        Output: return entity.getName().toLowerCase().contains("john doe");
                        
                        Input: "name contains Smith"
                        Output: return entity.getName().toLowerCase().contains("smith");
                        
                        Input: "email contains alice"
                        Output: return entity.getEmail().toLowerCase().contains("alice");
                        
                        Input: "email contains @company.com"
                        Output: return entity.getEmail().toLowerCase().contains("@company.com");
                        
                        Input: "active users"
                        Output: return entity.getLastWorkingDay() == null;
                        
                        Input: "former employees"
                        Output: return entity.getLastWorkingDay() != null;
                        
                        Input: "lastWorkingDay is not null"
                        Output: return entity.getLastWorkingDay() != null;
                        
                        Input: "firstWorkingDay in 2018"
                        Output: return entity.getFirstWorkingDay().getYear() == 2018;
                        
                        Input: "users starting after December 31 2020"
                        Output: return entity.getFirstWorkingDay().isAfter(LocalDate.of(2020, 12, 31));
                        
                        Input: "users starting before January 1 2020"
                        Output: return entity.getFirstWorkingDay().isBefore(LocalDate.of(2020, 1, 1));
                        
                        Input: "users starting in 2024"
                        Output: return entity.getFirstWorkingDay().getYear() == 2024;
                        
                        Input: "employees hired before January 1 2022"
                        Output: return entity.getFirstWorkingDay().isBefore(LocalDate.of(2022, 1, 1));
                        
                        Input: "employees hired more than 3 years ago"
                        Output: return entity.getFirstWorkingDay().isBefore(entity.getNow().minusYears(3));
                        
                        Input: "employees started within last 6 months"
                        Output: return entity.getFirstWorkingDay().isAfter(entity.getNow().minusMonths(6));
                        
                        Input: "users ending employment in 2024"
                        Output: return entity.getLastWorkingDay() != null && entity.getLastWorkingDay().getYear() == 2024;
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "users created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        """
        );
    }
}

