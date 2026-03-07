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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.location;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for Location entity AI filtering.
 */
public class LocationFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class LocationFilterDto {
                            private String    country; // never null
                            private String    state;   // never null
                            private LocalDate start;   // never null
                            private OffsetDateTime created; // never null
                            private OffsetDateTime updated; // never null
                        }
                        """,
                """
                        Special considerations for Locations:
                        - country and state fields contain location information for determining public holidays
                        - start dates indicate when the user began working at this location
                        - Support geographical searches and date-based filtering
                        - Remember: you are filtering LocationFilterDto entities, so each 'entity' is a LocationFilterDto
                        - When queries mention "locations created in 2024" - this means filter by creation year, NOT by checking if entity content contains "locations"
                        - Use only getter methods: entity.getCountry(), entity.getState(), entity.getStart(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "Germany"
                        Output: return entity.getCountry().toLowerCase().includes('germany');
                        
                        Input: "country is Germany"
                        Output: return entity.getCountry().toLowerCase().includes('germany');
                        
                        Input: "state Bavaria"
                        Output: return entity.getState().toLowerCase().includes('bavaria');
                        
                        Input: "California"
                        Output: return entity.getState().toLowerCase().includes('california');
                        
                        Input: "locations in Australia"
                        Output: return entity.getCountry().toLowerCase().includes('australia');
                        
                        Input: "locations starting after January 31 2024"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2024, 1, 31); return entity.getStart().isAfter(refDate);
                        
                        Input: "locations starting before March 1 2024"
                        Output: const refDate = Java.type('java.time.LocalDate').of(2024, 3, 1); return entity.getStart().isBefore(refDate);
                        
                        Input: "locations starting in 2025"
                        Output: return entity.getStart().getYear() === 2025;
                        
                        Input: "locations starting in June, July or August 2024"
                        Output: const month = entity.getStart().getMonthValue(); const year = entity.getStart().getYear(); return year === 2024 && (month === 6 || month === 7 || month === 8);
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "locations created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        """,
                """
                        Examples:
                        Input: "Germany"
                        Output: return entity.getCountry().toLowerCase().contains("germany");
                        
                        Input: "country is Germany"
                        Output: return entity.getCountry().toLowerCase().contains("germany");
                        
                        Input: "state Bavaria"
                        Output: return entity.getState().toLowerCase().contains("bavaria");
                        
                        Input: "California"
                        Output: return entity.getState().toLowerCase().contains("california");
                        
                        Input: "locations in Australia"
                        Output: return entity.getCountry().toLowerCase().contains("australia");
                        
                        Input: "locations starting after January 31 2024"
                        Output: return entity.getStart().isAfter(LocalDate.of(2024, 1, 31));
                        
                        Input: "locations starting before March 1 2024"
                        Output: return entity.getStart().isBefore(LocalDate.of(2024, 3, 1));
                        
                        Input: "locations starting in 2025"
                        Output: return entity.getStart().getYear() == 2025;
                        
                        Input: "locations starting in June, July or August 2024"
                        Output: return entity.getStart().getYear() == 2024 && (entity.getStart().getMonthValue() == 6 || entity.getStart().getMonthValue() == 7 || entity.getStart().getMonthValue() == 8);
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "locations created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        """
        );
    }
}

