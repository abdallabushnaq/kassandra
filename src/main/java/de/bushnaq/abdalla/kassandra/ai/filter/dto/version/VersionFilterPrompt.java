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

package de.bushnaq.abdalla.kassandra.ai.filter.dto.version;

import de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig;

/**
 * Configuration for Version entity AI filtering.
 */
public class VersionFilterPrompt {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class VersionFilterDto {
                            private String         name;      // never null – semantic version string (e.g. "1.2.3", "3.0.0-beta")
                            private Long           productId; // never null – owning product
                            private OffsetDateTime created;   // never null
                            private OffsetDateTime updated;   // never null
                        }
                        """,
                """
                        Special considerations for Versions:
                        - Version names follow semantic versioning: MAJOR.MINOR.PATCH (e.g. "1.0.0", "2.1.5")
                        - Pre-release suffixes: -alpha, -beta, -rc1, -SNAPSHOT (case-insensitive)
                        - Pre-release versions have a hyphen in the name; stable releases do not
                        - Remember: you are filtering VersionFilterDto entities, so each 'entity' is a VersionFilterDto
                        - When queries mention "versions created in 2024" - this means filter by creation year
                        - Terms like "versions", "releases" refer to the entity type, not name content
                        - Use only getter methods: entity.getName(), entity.getProductId(), entity.getCreated(), entity.getUpdated()
                        - Never use reflection or field access, always use public getter methods
                        
                        VERSION NUMBER COMPARISONS:
                        - To compare versions numerically, convert to a weighted integer: major*10000 + minor*100 + patch
                        - Strip any pre-release suffix before parsing patch: "3.0.0-beta" → patch=0
                        - Pre-release versions are considered lower than the same MAJOR.MINOR.PATCH without a suffix
                        """,
                """
                        Examples:
                        Input: "1.0.0"
                        Output: return entity.getName().toLowerCase().includes('1.0.0');
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().includes('beta');
                        
                        Input: "alpha"
                        Output: return entity.getName().toLowerCase().includes('alpha');
                        
                        Input: "snapshot"
                        Output: return entity.getName().toLowerCase().includes('snapshot');
                        
                        Input: "rc"
                        Output: return entity.getName().toLowerCase().includes('rc');
                        
                        Input: "pre-release versions"
                        Output: return entity.getName().includes('-');
                        
                        Input: "stable versions (no pre-release suffix)"
                        Output: return !entity.getName().includes('-');
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "versions created in 2025"
                        Output: return entity.getCreated().getYear() === 2025;
                        
                        Input: "versions created after January 31 2024"
                        Output: const boundary = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, Java.type('java.time.ZoneOffset').UTC); return entity.getCreated().isAfter(boundary);
                        
                        Input: "versions created before March 1 2024"
                        Output: const boundary = Java.type('java.time.OffsetDateTime').of(2024, 3, 1, 0, 0, 0, 0, Java.type('java.time.ZoneOffset').UTC); return entity.getCreated().isBefore(boundary);
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "versions with major version 3"
                        Output: return entity.getName().startsWith('3.');
                        
                        Input: "versions with name starting with 1."
                        Output: return entity.getName().startsWith('1.');
                        
                        Input: "version greater than 2.0.0"
                        Output: function versionValue(n) { const p = n.split('.'); if (p.length < 3) return -1; const patch = parseInt(p[2].includes('-') ? p[2].substring(0, p[2].indexOf('-')) : p[2]); return parseInt(p[0])*10000 + parseInt(p[1])*100 + patch; } return versionValue(entity.getName()) > 20000;
                        
                        Input: "versions between 1.0.0 and 3.0.0 inclusive"
                        Output: function versionValue(n) { const p = n.split('.'); if (p.length < 3) return -1; const patch = parseInt(p[2].includes('-') ? p[2].substring(0, p[2].indexOf('-')) : p[2]); return parseInt(p[0])*10000 + parseInt(p[1])*100 + patch; } const v = versionValue(entity.getName()); return v >= 10000 && v <= 30000;
                        """,
                """
                        Examples:
                        Input: "1.0.0"
                        Output: return entity.getName().toLowerCase().contains("1.0.0");
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().contains("beta");
                        
                        Input: "alpha"
                        Output: return entity.getName().toLowerCase().contains("alpha");
                        
                        Input: "snapshot"
                        Output: return entity.getName().toLowerCase().contains("snapshot");
                        
                        Input: "rc"
                        Output: return entity.getName().toLowerCase().contains("rc");
                        
                        Input: "pre-release versions"
                        Output: return entity.getName().contains("-");
                        
                        Input: "stable versions (no pre-release suffix)"
                        Output: return !entity.getName().contains("-");
                        
                        Input: "created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "versions created in 2025"
                        Output: return entity.getCreated().getYear() == 2025;
                        
                        Input: "versions created after January 31 2024"
                        Output: return entity.getCreated().isAfter(OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC));
                        
                        Input: "versions created before March 1 2024"
                        Output: return entity.getCreated().isBefore(OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC));
                        
                        Input: "updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "versions with major version 3"
                        Output: return entity.getName().startsWith("3.");
                        
                        Input: "version greater than 2.0.0"
                        Output: String[] p = entity.getName().split("\\."); if (p.length < 3) return false; try { int patch = Integer.parseInt(p[2].contains("-") ? p[2].substring(0, p[2].indexOf("-")) : p[2]); int v = Integer.parseInt(p[0])*10000 + Integer.parseInt(p[1])*100 + patch; return v > 20000; } catch (NumberFormatException e) { return false; }
                        
                        Input: "versions between 1.0.0 and 3.0.0 inclusive"
                        Output: String[] p = entity.getName().split("\\."); if (p.length < 3) return false; try { int patch = Integer.parseInt(p[2].contains("-") ? p[2].substring(0, p[2].indexOf("-")) : p[2]); int v = Integer.parseInt(p[0])*10000 + Integer.parseInt(p[1])*100 + patch; return v >= 10000 && v <= 30000; } catch (NumberFormatException e) { return false; }
                        """
        );
    }
}

