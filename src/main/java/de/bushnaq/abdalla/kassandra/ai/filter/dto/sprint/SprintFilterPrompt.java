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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.sprint;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for Sprint entity AI filtering.
 */
public class SprintFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class SprintFilterDto {
                            private String        name;                    // never null
                            private String        status;                  // never null; one of: CREATED, STARTED, CLOSED
                            private LocalDateTime start;                   // may be null
                            private LocalDateTime end;                     // may be null
                            private LocalDateTime releaseDate;             // may be null
                            private long          originalEstimationHours; // original estimation in whole hours
                            private long          workedHours;             // hours already worked
                            private long          remainingHours;          // remaining hours
                            private OffsetDateTime created;                // never null
                            private OffsetDateTime updated;                // never null
                        }
                        """,
                """
                        Special considerations for Sprints:
                        - status is a plain String with one of three values: "CREATED", "STARTED", "CLOSED"
                        - Compare status with a string literal: entity.getStatus() === 'STARTED'  (no Java.type() needed)
                        - originalEstimationHours, workedHours, remainingHours are plain long values — compare directly: entity.getOriginalEstimationHours() > 100
                        - start and end are LocalDateTime — use Java.type('java.time.LocalDateTime') for date constants
                        - Sprint names often include version numbers and suffixes (e.g., "Sprint 1.2.3-Alpha", "Sprint 2.0.0-RC1", "Sprint 3.0.0-SNAPSHOT")
                        - Remember: you are filtering SprintFilterDto entities, so each 'entity' is a SprintFilterDto
                        - When queries mention "sprints created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "sprints"
                        - Terms like "sprints", "items", or similar generic terms refer to the entity type, not name content
                        - Use only getter methods: entity.getName(), entity.getStatus(), entity.getStart(), entity.getEnd(), entity.getOriginalEstimationHours(), entity.getWorkedHours(), entity.getRemainingHours(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "sprint alpha"
                        Output: return entity.getName().toLowerCase().includes('alpha');
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().includes('beta');
                        
                        Input: "status is CREATED"
                        Output: return entity.getStatus() === 'CREATED';
                        
                        Input: "status is STARTED"
                        Output: return entity.getStatus() === 'STARTED';
                        
                        Input: "status is CLOSED"
                        Output: return entity.getStatus() === 'CLOSED';
                        
                        Input: "sprints with remaining work"
                        Output: return entity.getRemainingHours() > 0;
                        
                        Input: "sprints with no remaining work"
                        Output: return entity.getRemainingHours() === 0;
                        
                        Input: "sprints over 100 hours estimation"
                        Output: return entity.getOriginalEstimationHours() > 100;
                        
                        Input: "sprints with more than 40 hours worked"
                        Output: return entity.getWorkedHours() > 40;
                        
                        Input: "sprints between 80 and 150 hours original estimation"
                        Output: return entity.getOriginalEstimationHours() >= 80 && entity.getOriginalEstimationHours() <= 150;
                        
                        Input: "sprints starting after January 31 2024"
                        Output: const refDate = Java.type('java.time.LocalDateTime').of(2024, 1, 31, 23, 59, 59); return entity.getStart() !== null && entity.getStart().isAfter(refDate);
                        
                        Input: "sprints ending before March 1 2024"
                        Output: const refDate = Java.type('java.time.LocalDateTime').of(2024, 3, 1, 0, 0, 0); return entity.getEnd() !== null && entity.getEnd().isBefore(refDate);
                        
                        Input: "sprints starting in 2025"
                        Output: return entity.getStart() !== null && entity.getStart().getYear() === 2025;
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "sprints created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        """,
                """
                        Examples:
                        Input: "sprint alpha"
                        Output: return entity.getName().toLowerCase().contains("alpha");
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().contains("beta");
                        
                        Input: "status is CREATED"
                        Output: return entity.getStatus().equals("CREATED");
                        
                        Input: "status is STARTED"
                        Output: return entity.getStatus().equals("STARTED");
                        
                        Input: "status is CLOSED"
                        Output: return entity.getStatus().equals("CLOSED");
                        
                        Input: "sprints with remaining work"
                        Output: return entity.getRemainingHours() > 0;
                        
                        Input: "sprints with no remaining work"
                        Output: return entity.getRemainingHours() == 0;
                        
                        Input: "sprints over 100 hours estimation"
                        Output: return entity.getOriginalEstimationHours() > 100;
                        
                        Input: "sprints with more than 40 hours worked"
                        Output: return entity.getWorkedHours() > 40;
                        
                        Input: "sprints between 80 and 150 hours original estimation"
                        Output: return entity.getOriginalEstimationHours() >= 80 && entity.getOriginalEstimationHours() <= 150;
                        
                        Input: "sprints starting after January 31 2024"
                        Output: return entity.getStart() != null && entity.getStart().isAfter(LocalDateTime.of(2024, 1, 31, 23, 59, 59));
                        
                        Input: "sprints ending before March 1 2024"
                        Output: return entity.getEnd() != null && entity.getEnd().isBefore(LocalDateTime.of(2024, 3, 1, 0, 0, 0));
                        
                        Input: "sprints starting in 2025"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == 2025;
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "sprints created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        """
        );
    }
}

