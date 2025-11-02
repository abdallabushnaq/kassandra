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
 * Configuration for Sprint entity AI filtering
 */
public class SprintConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Sprint {
                            private String name;//never null
                            private LocalDateTime start;
                            private LocalDateTime end;
                            private LocalDateTime releaseDate;
                            private Status status;//never null
                            private Duration originalEstimation;
                            private Duration worked;
                            private Duration remaining;
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Sprints:
                        - Sprint names often include version numbers, alpha/beta/rc suffixes (e.g., "Sprint 1.2.3-Alpha", "Authentication Sprint")
                        - Status is an enum with following values: CREATED, STARTED, CLOSED.
                        - Time durations are Duration objects (originalEstimation, worked, remaining)
                        - Sprint keys follow patterns like S-1, S-123. Keys are basically just unique database IDs of the Sprint entity.
                        - Support time-based queries (start/end dates, duration comparisons)
                        - Consider sprint progress (worked vs remaining time)
                        - Sprints belong to features and are assigned to users
                        - Remember: you are filtering Sprint entities, so each 'entity' is already a Sprint
                        - When queries mention "sprints created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "sprints"
                        - Terms like "sprints", "items", or similar generic terms refer to the entity type, not name content
                        - Use only getter methods like entity.getName(), entity.getCreated(), entity.getUpdated(), entity.getFeatureId(), entity.getStatus(), entity.getStart(), entity.getEnd(), entity.getOriginalEstimation(), entity.getWorked(), entity.getRemaining()
                        - Never use reflection or field access, always use public getter methods
                        """,
                """
                        Examples:
                        Input: "sprint alpha"
                        Output: return entity.getName().toLowerCase().includes('alpha');
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().includes('beta');
                        
                        Input: "authentication"
                        Output: return entity.getName().toLowerCase().includes('authentication');
                        
                        Input: "created sprints"
                        Output: const Status = Java.type('de.bushnaq.abdalla.kassandra.dto.Status'); return entity.getStatus() === Status.CREATED;
                        
                        Input: "started sprints"
                        Output: const Status = Java.type('de.bushnaq.abdalla.kassandra.dto.Status'); return entity.getStatus() === Status.STARTED;
                        
                        Input: "closed sprints"
                        Output: const Status = Java.type('de.bushnaq.abdalla.kassandra.dto.Status'); return entity.getStatus() === Status.CLOSED;
                        
                        Input: "sprints starting after January 2025"
                        Output: if (!entity.getStart()) return false; const refDate = Java.type('java.time.LocalDateTime').of(2025, 1, 31, 23, 59, 59); return entity.getStart().isAfter(refDate);
                        
                        Input: "sprints ending before March 2025"
                        Output: if (!entity.getEnd()) return false; const refDate = Java.type('java.time.LocalDateTime').of(2025, 3, 1, 0, 0, 0); return entity.getEnd().isBefore(refDate);
                        
                        Input: "sprints with remaining work"
                        Output: if (!entity.getRemaining()) return false; return !entity.getRemaining().isZero();
                        
                        Input: "sprints over 60 hours estimation"
                        Output: if (!entity.getOriginalEstimation()) return false; return entity.getOriginalEstimation().toHours() > 60;
                        
                        Input: "sprints with more than 40 hours worked"
                        Output: if (!entity.getWorked()) return false; return entity.getWorked().toHours() > 40;
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "sprints created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "sprints starting this year"
                        Output: if (!entity.getStart()) return false; const currentYear = Java.type('java.time.Year').now().getValue(); return entity.getStart().getYear() === currentYear;
                        
                        Input: "sprints ending this month"
                        Output: if (!entity.getEnd()) return false; const now = Java.type('java.time.LocalDateTime').now(); return entity.getEnd().getYear() === now.getYear() && entity.getEnd().getMonth() === now.getMonth();
                        """,
                """
                        Examples:
                        Input: "sprint alpha"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("alpha");
                        
                        Input: "name contains beta"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("beta");
                        
                        Input: "authentication"
                        Output: return entity.getName() != null && entity.getName().toLowerCase().contains("authentication");
                        
                        Input: "created sprints"
                        Output: return entity.getStatus() != null && entity.getStatus() == Status.CREATED;
                        
                        Input: "started sprints"
                        Output: return entity.getStatus() != null && entity.getStatus() == Status.STARTED;
                        
                        Input: "closed sprints"
                        Output: return entity.getStatus() != null && entity.getStatus() == Status.CLOSED;
                        
                        Input: "sprints starting after January 2025"
                        Output: return entity.getStart() != null && entity.getStart().isAfter(LocalDateTime.of(2025, 1, 31, 23, 59, 59));
                        
                        Input: "sprints ending before March 2025"
                        Output: return entity.getEnd() != null && entity.getEnd().isBefore(LocalDateTime.of(2025, 3, 1, 0, 0, 0));
                        
                        Input: "sprints with remaining work"
                        Output: return entity.getRemaining() != null && !entity.getRemaining().isZero();
                        
                        Input: "sprints over 60 hours estimation"
                        Output: return entity.getOriginalEstimation() != null && entity.getOriginalEstimation().toHours() > 60;
                        
                        Input: "sprints with more than 40 hours worked"
                        Output: return entity.getWorked() != null && entity.getWorked().toHours() > 40;
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated() != null && entity.getCreated().getYear() == 2024;
                        
                        Input: "sprints created in 2024"
                        Output: return entity.getCreated() != null && entity.getCreated().getYear() == 2024;
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated() != null && entity.getUpdated().getYear() == 2025;
                        
                        Input: "sprints starting this year"
                        Output: return entity.getStart() != null && entity.getStart().getYear() == java.time.Year.now().getValue();
                        
                        Input: "sprints ending this month"
                        Output: return entity.getEnd() != null && entity.getEnd().getYear() == java.time.LocalDateTime.now().getYear() && entity.getEnd().getMonth() == java.time.LocalDateTime.now().getMonth();
                        """
        );
    }
}
