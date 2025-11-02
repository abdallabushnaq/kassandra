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
 * Configuration for Product entity AI filtering
 */
public class ProductConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Product {
                            private String name;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Products:
                        - Remember: you are filtering Product entities, so each 'entity' is a Product.
                        - When queries mention "products created in 2024" - this means filter by creation year.
                        - Terms like "products", "items", or similar generic terms refer to the entity type, not name content.
                        - Use only getter methods like entity.getName(), entity.getCreated(), entity.getUpdated().
                        """,
                """
                        Examples:
                        Input: "Orion"
                        Output: return entity.getName().toLowerCase().includes('orion');
                        
                        Input: "name contains project"
                        Output: return entity.getName().toLowerCase().includes('project');
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "products created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "items created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "products created after January 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, entity.getCreated().getOffset()); return entity.getCreated().isAfter(refDate);
                        
                        Input: "items created before December 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 12, 1, 0, 0, 0, 0, entity.getCreated().getOffset()); return entity.getCreated().isBefore(refDate);
                        
                        Input: "products created this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getCreated().getYear() === currentYear;
                        
                        Input: "updated in 2024"
                        Output: return entity.getUpdated().getYear() === 2024;
                        
                        Input: "products updated in 2024"
                        Output: return entity.getUpdated().getYear() === 2024;
                        
                        Input: "items updated in 2024"
                        Output: return entity.getUpdated().getYear() === 2024;
                        
                        Input: "products updated after January 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, entity.getUpdated().getOffset()); return entity.getUpdated().isAfter(refDate);
                        
                        Input: "items updated before December 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 12, 1, 0, 0, 0, 0, entity.getUpdated().getOffset()); return entity.getUpdated().isBefore(refDate);
                        
                        Input: "products updated this year"
                        Output: const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getUpdated().getYear() === currentYear;
                        
                        Input: "MARS"
                        Output: return entity.getName().toLowerCase().includes('mars');
                        
                        Input: "space products created in 2024"
                        Output: const hasSpace = entity.getName().toLowerCase().includes('space'); const isCreated2024 = entity.getCreated().getYear() === 2024; return hasSpace && isCreated2024;""",
                """
                        Examples:
                        Input: "Orion"
                        Output: return entity.getName().toLowerCase().contains("orion");
                        
                        Input: "name contains project"
                        Output: return entity.getName().toLowerCase().contains("project");
                        
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "products created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "items created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "products created after January 2024"
                        Output: return entity.getCreated().isAfter(OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "items created before December 2024"
                        Output: return entity.getCreated().isBefore(OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "products created this year"
                        Output: return entity.getCreated().getYear() == java.time.Year.now().getValue();
                        
                        
                        Input: "updated in 2024"
                        Output: return entity.getUpdated().getYear() == 2024;
                        
                        Input: "products updated in 2024"
                        Output: return entity.getUpdated().getYear() == 2024;
                        
                        Input: "items updated in 2024"
                        Output: return entity.getUpdated().getYear() == 2024;
                        
                        Input: "products updated after January 2024"
                        Output: return entity.getUpdated().isAfter(OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "items updated before December 2024"
                        Output: return entity.getUpdated().isBefore(OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "products updated this year"
                        Output: return entity.getUpdated().getYear() == java.time.Year.now().getValue();
                        
                        """
        );
    }
}
