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
 * Configuration for Feature entity AI filtering
 */
public class FeatureConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Feature {
                            private String name;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Features:
                        - Feature names describe functionality (e.g., "User Authentication", "Payment Processing")
                        - Feature keys follow patterns like F-1, F-123. Keys are basically just unique database IDs of the Feature entity.
                        - Features are grouped under versions and contain sprints
                        - Focus on feature purpose and functionality descriptions
                        - Remember: you are filtering Feature entities, so each 'entity' is already a Feature
                        - When queries mention "features created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "features"
                        - Terms like "features", "items", or similar generic terms refer to the entity type, not name content
                        - Use only getter methods like entity.getName(), entity.getCreated(), entity.getUpdated(), entity.getId(), entity.getVersionId(), entity.getKey()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "authentication"
                        Output: return entity.getName().toLowerCase().includes('authentication');
                        
                        Input: "name contains user"
                        Output: return entity.getName().toLowerCase().includes('user');
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "features created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "items created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "features created after January 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, entity.getCreated().getOffset()); return entity.getCreated().isAfter(refDate);
                        
                        Input: "features created before December 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 12, 1, 0, 0, 0, 0, entity.getCreated().getOffset()); return entity.getCreated().isBefore(refDate);
                        
                        Input: "features created this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getCreated().getYear() === currentYear;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "features updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "items updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "features updated after January 2025"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2025, 1, 31, 23, 59, 59, 0, entity.getUpdated().getOffset()); return entity.getUpdated().isAfter(refDate);
                        
                        Input: "features updated before December 2025"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2025, 12, 1, 0, 0, 0, 0, entity.getUpdated().getOffset()); return entity.getUpdated().isBefore(refDate);
                        
                        Input: "features updated this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getUpdated().getYear() === currentYear;
                        
                        Input: "payment features"
                        Output: return entity.getName().toLowerCase().includes('payment');
                        
                        Input: "authentication features created in 2024"
                        Output: const hasAuth = entity.getName().toLowerCase().includes('authentication'); const isCreated2024 = entity.getCreated().getYear() === 2024; return hasAuth && isCreated2024;
                        """,
                """
                        Examples:
                        Input: "authentication"
                        Output: return entity.getName().toLowerCase().contains("authentication");
                        
                        Input: "name contains user"
                        Output: return entity.getName().toLowerCase().contains("user");
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "features created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "features created after January 2024"
                        Output: return entity.getCreated().isAfter(OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "features created before December 2024"
                        Output: return entity.getCreated().isBefore(OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "features updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "payment features"
                        Output: return entity.getName().toLowerCase().contains("payment");
                        """
        );
    }
}
