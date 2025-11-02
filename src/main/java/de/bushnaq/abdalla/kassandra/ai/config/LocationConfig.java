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
 * Configuration for Location entity AI filtering
 */
public class LocationConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Location extends AbstractTimeAware {
                            private String country;//never null
                            private LocalDate start;//never null
                            private String state;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Locations:
                        - Country and state fields contain location information for determining public holidays
                        - Start dates indicate when the user began working at this location
                        - Support geographical searches and date-based filtering
                        - Consider legal/contract location contexts for employment purposes
                        - Remember: you are filtering Location entities, so each 'entity' is already a Location
                        - When queries mention "locations created in 2024" - this means filter by creation year, NOT by checking if entity content contains "locations"
                        - Use only getter methods like entity.getCountry(), entity.getState(), entity.getStart(), entity.getUser(), entity.getCreated(), entity.getUpdated(), entity.getId(), entity.getKey()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "Germany"
                        Output: return entity && entity.getCountry() && entity.getCountry().toLowerCase().includes('germany');
                        
                        Input: "country Germany"
                        Output: return entity && entity.getCountry() && entity.getCountry().toLowerCase().includes('germany');
                        
                        Input: "state Bavaria"
                        Output: return entity && entity.getState() && entity.getState().toLowerCase().includes('bavaria');
                        
                        Input: "locations in Australia"
                        Output: return entity && entity.getCountry() && entity.getCountry().toLowerCase().includes('australia');
                        
                        Input: "locations starting after January 2025"
                        Output: if (!entity || !entity.getStart()) return false; const refDate = Java.type('java.time.LocalDate').of(2025, 1, 31); return entity.getStart().isAfter(refDate);
                        
                        Input: "locations starting before March 2025"
                        Output: if (!entity || !entity.getStart()) return false; const refDate = Java.type('java.time.LocalDate').of(2025, 3, 1); return entity.getStart().isBefore(refDate);
                        
                        Input: "locations starting in 2025"
                        Output: return entity && entity.getStart() && entity.getStart().getYear() === 2025;
                        
                        Input: "locations starting this year"
                        Output: if (!entity || !entity.getStart()) return false; const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getStart().getYear() === currentYear;
                        
                        Input: "European locations"
                        Output: if (!entity || !entity.getCountry()) return false; const country = entity.getCountry().toLowerCase(); return country.includes('germany') || country.includes('france') || country.includes('italy') || country.includes('spain') || country.includes('netherlands') || country.includes('belgium') || country.includes('austria') || country.includes('switzerland') || country.includes('united kingdom') || country.includes('ireland') || country.includes('portugal') || country.includes('greece') || country.includes('denmark') || country.includes('sweden') || country.includes('norway') || country.includes('finland') || country.includes('poland');
                        
                        Input: "US locations"
                        Output: if (!entity || !entity.getCountry()) return false; const country = entity.getCountry().toLowerCase(); return country.includes('united states') || country.includes('usa') || country.includes('america');
                        
                        Input: "Asian locations"
                        Output: if (!entity || !entity.getCountry()) return false; const country = entity.getCountry().toLowerCase(); return country.includes('japan') || country.includes('china') || country.includes('india') || country.includes('korea') || country.includes('singapore') || country.includes('thailand') || country.includes('vietnam') || country.includes('malaysia') || country.includes('indonesia');
                        
                        Input: "created in 2025"
                        Output: return entity && entity.getCreated() && entity.getCreated().getYear() === 2025;
                        
                        Input: "locations created in 2025"
                        Output: return entity && entity.getCreated() && entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity && entity.getUpdated() && entity.getUpdated().getYear() === 2025;
                        
                        Input: "California"
                        Output: return entity && entity.getState() && entity.getState().toLowerCase().includes('california');
                        
                        Input: "Texas locations"
                        Output: return entity && entity.getState() && entity.getState().toLowerCase().includes('texas');""",
                """
                        Examples:
                        Input: "Germany"
                        Output: return entity.getCountry().toLowerCase().contains("germany");
                        
                        Input: "country Germany"
                        Output: return entity.getCountry().toLowerCase().contains("germany");
                        
                        Input: "state Bavaria"
                        Output: return entity.getState().toLowerCase().contains("bavaria");
                        
                        Input: "locations in Australia"
                        Output: return entity.getCountry().toLowerCase().contains("australia");
                        
                        Input: "locations starting after January 2025"
                        Output: return entity.getStart() != null && entity.getStart().isAfter(LocalDate.of(2025, 1, 31));
                        
                        Input: "locations starting before March 2025"
                        Output: return entity.getStart() != null && entity.getStart().isBefore(LocalDate.of(2025, 3, 1));
                        
                        Input: "locations starting in 2025"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == 2025;
                        
                        Input: "locations starting this year"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == java.time.Year.now().getValue();
                        
                        Input: "European locations"
                        Output: return (entity.getCountry().toLowerCase().contains("germany") || entity.getCountry().toLowerCase().contains("france") || entity.getCountry().toLowerCase().contains("italy") || entity.getCountry().toLowerCase().contains("spain") || entity.getCountry().toLowerCase().contains("netherlands") || entity.getCountry().toLowerCase().contains("belgium") || entity.getCountry().toLowerCase().contains("austria") || entity.getCountry().toLowerCase().contains("switzerland") || entity.getCountry().toLowerCase().contains("united kingdom") || entity.getCountry().toLowerCase().contains("ireland") || entity.getCountry().toLowerCase().contains("portugal") || entity.getCountry().toLowerCase().contains("greece") || entity.getCountry().toLowerCase().contains("denmark") || entity.getCountry().toLowerCase().contains("sweden") || entity.getCountry().toLowerCase().contains("norway") || entity.getCountry().toLowerCase().contains("finland") || entity.getCountry().toLowerCase().contains("poland"));
                        
                        Input: "US locations"
                        Output: return (entity.getCountry().toLowerCase().contains("united states") || entity.getCountry().toLowerCase().contains("usa") || entity.getCountry().toLowerCase().contains("america"));
                        
                        Input: "Asian locations"
                        Output: return (entity.getCountry().toLowerCase().contains("japan") || entity.getCountry().toLowerCase().contains("china") || entity.getCountry().toLowerCase().contains("india") || entity.getCountry().toLowerCase().contains("korea") || entity.getCountry().toLowerCase().contains("singapore") || entity.getCountry().toLowerCase().contains("thailand") || entity.getCountry().toLowerCase().contains("vietnam") || entity.getCountry().toLowerCase().contains("malaysia") || entity.getCountry().toLowerCase().contains("indonesia"));
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "locations created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "California"
                        Output: return entity.getState().toLowerCase().contains("california");
                        
                        Input: "Texas locations"
                        Output: return entity.getState().toLowerCase().contains("texas");"""
        );
    }
}
