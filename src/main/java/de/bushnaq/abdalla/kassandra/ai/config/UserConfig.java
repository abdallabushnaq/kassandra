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
 * Configuration for User entity AI filtering
 */
public class UserConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class User {
                            LocalDate now;//never null, injected with current date
                            private String name;//never null
                            private String email;//never null
                            private LocalDate firstWorkingDay;//never null
                            private LocalDate lastWorkingDay; // null for active users
                            private Color color;//never null
                            private OffsetDateTime created;//never null, indicates only when this entity was created
                            private OffsetDateTime updated;//never null, indicates only when this entity was updated
                        }
                        """,
                """
                        Special considerations for Users:
                        - User names contain first and last names (e.g., "John Doe", "Jane Smith")
                        - Email addresses follow standard patterns (firstname.lastname@domain.com)
                        - Employment status: active users have lastWorkingDay as null, former employees have a date
                        - firstWorkingDay indicates hire date, the date an employee starts his contract, lastWorkingDay indicates termination date
                        - Users have associated availabilities, locations, and off days
                        - Support tenure-based queries and employment status filtering
                        - Remember: you are filtering User entities, so each 'entity' is already a User
                        - When queries mention "users created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "users"
                        - Terms like "users", "employees", or similar generic terms refer to the entity type, not name content
                        - Use only getter methods like entity.getName(), entity.getEmail(), entity.getFirstWorkingDay(), entity.getLastWorkingDay(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "John Doe"
                        Output: return entity.getName().toLowerCase().includes('john doe');
                        
                        Input: "first name John"
                        Output: return entity.getName().toLowerCase().includes('john');
                        
                        Input: "last name Smith"
                        Output: return entity.getName().toLowerCase().includes('smith');
                        
                        Input: "name contains Doe"
                        Output: return entity.getName().toLowerCase().includes('doe');
                        
                        Input: "email john.doe@company.com"
                        Output: return entity.getEmail().toLowerCase() === 'john.doe@company.com';
                        
                        Input: "email contains @company.com"
                        Output: return entity.getEmail().toLowerCase().includes('@company.com');
                        
                        Input: "active users"
                        Output: return entity.getLastWorkingDay() === null;
                        
                        Input: "former employees"
                        Output: return entity.getLastWorkingDay() !== null;
                        
                        Input: "users starting after 2020"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2020, 12, 31); return entity.getFirstWorkingDay().isAfter(refDate);
                        
                        Input: "users starting before 2020"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2020, 1, 1); return entity.getFirstWorkingDay().isBefore(refDate);
                        
                        Input: "users starting in 2024"
                        Output: return entity.getFirstWorkingDay().getYear() === 2024;
                        
                        Input: "long-term employees"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2021, 1, 1); return entity.getFirstWorkingDay().isBefore(refDate);
                        
                        Input: "new employees"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2023, 12, 31); return entity.getFirstWorkingDay().isAfter(refDate);
                        
                        Input: "users hired this year"
                        Output: const currentYear = now.getYear(); return entity.getFirstWorkingDay().getYear() === currentYear;
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "users created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "employees created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "users ending employment in 2024"
                        Output: return entity.getLastWorkingDay() && entity.getLastWorkingDay().getYear() === 2024;
                        
                        Input: "users ending employment after June 2024"
                        Output: if (!entity.getLastWorkingDay()) return false; const refDate = Java.type('java.time.LocalDate').of(2024, 6, 30); return entity.getLastWorkingDay().isAfter(refDate);
                        
                        Input: "employees since over 4 years"
                        Output: return entity.getFirstWorkingDay().isBefore(now.minusYears(4));
                        
                        Input: "employees started within last 6 months"
                        Output: return entity.getFirstWorkingDay().isAfter(now.minusMonths(6));
                        """,
                """
                        Examples:
                        Input: "John Doe"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("john doe");
                        
                        Input: "first name John"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("john");
                        
                        Input: "last name Smith"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("smith");
                        
                        Input: "name contains Doe"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("doe");
                        
                        Input: "email john.doe@company.com"
                        Output: return entity.getEmail() != null && entity.getEmail().toLowerCase().equals("john.doe@company.com");
                        
                        Input: "email contains @company.com"
                        Output: return entity.getEmail() != null && entity.getEmail().toLowerCase().contains("@company.com");
                        
                        Input: "active users"
                        Output: return entity.getLastWorkingDay() == null;
                        
                        Input: "former employees"
                        Output: return entity.getLastWorkingDay() != null;
                        
                        Input: "users starting after 2020"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().isAfter(LocalDate.of(2020, 12, 31));
                        
                        Input: "users starting before 2020"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().isBefore(LocalDate.of(2020, 1, 1));
                        
                        Input: "users starting in 2024"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().getYear() == 2024;
                        
                        Input: "long-term employees"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().isBefore(LocalDate.of(2021, 1, 1));
                        
                        Input: "new employees"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().isAfter(LocalDate.of(2023, 12, 31));
                        
                        Input: "users hired this year"
                        Output: return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().getYear() == getNow().getYear();
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "users created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "employees created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "users ending employment in 2024"
                        Output: return entity.getLastWorkingDay() != null && entity.getLastWorkingDay().getYear() == 2024;
                        
                        Input: "users ending employment after June 2024"
                        Output: if (!entity.getLastWorkingDay() || !entity.getNow()) return false; return entity.getLastWorkingDay().isAfter(LocalDate.of(2024, 6, 30));
                        
                        Input: "employees since over 4 years"
                        Output: if (!entity.getFirstWorkingDay() || !entity.getNow()) return false; return entity.getFirstWorkingDay().isBefore(entity.getNow().minusYears(4));
                        
                        Input: "employees started within last 6 months"
                        Output: if (!entity.getFirstWorkingDay() || !entity.getNow()) return false; return entity.getFirstWorkingDay().isAfter(entity.getNow().minusMonths(6));
                        """
        );
    }
}
