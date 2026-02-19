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
 * Configuration for Version entity AI filtering
 */
public class VersionConfig {

    public static PromptConfig getConfig() {
        return new PromptConfig(
                """
                        @Getter
                        @Setter
                        public class Version {
                            private String name;//never null
                            private OffsetDateTime created;//never null
                            private OffsetDateTime updated;//never null
                        }
                        """,
                """
                        Special considerations for Versions:
                        - Focus on version names (semantic versions like "1.0.0", "2.1.5") and creation/update dates
                        - Remember: you are filtering Version entities, so each 'entity' is already a Version
                        - When queries mention "versions created in 2024" - this means filter by creation year, NOT by checking if entity.name contains "versions"
                        - Terms like "versions", "releases", or similar generic terms refer to the entity type, not name content
                        - Version names often follow semantic versioning (major.minor.patch) or include suffixes like "-beta", "-alpha"
                        - Use only getter methods like entity.getName(), entity.getCreated(), entity.getUpdated(), entity.getId(), entity.getProductId()
                        - Never use reflection or field access, always use public getter methods
                        
                        VERSION NUMBER COMPARISONS:
                        - Support version comparisons (greater than, less than, equal, between) by comparing semantic version numbers numerically.
                        - Version format: MAJOR.MINOR.PATCH (e.g., "3.1.4", "1.5.6", "2.0.0").
                        - To compare versions numerically, convert each version to a comparable number using positional weighting:
                          * Split version by dots: "3.1.4" †' [3, 1, 4]
                          * Calculate weighted value: major*10000 + minor*100 + patch*1
                          * Example: "3.1.4" †' 3*10000 + 1*100 + 4*1 = 30104
                          * Example: "1.5.6" †' 1*10000 + 5*100 + 6*1 = 10506
                          * Therefore: 30104 > 10506, so "3.1.4" > "1.5.6"
                        - For versions with suffixes like "-beta", "-alpha", treat them as lower than their base version.
                        - pre-release versions have suffixes like "-alpha", "-beta", "-rc1", "rc2" and "-snapshot" and are considered lower than the corresponding release version (e.g., "1.0.0-beta" < "1.0.0").
                        - a stable version is the opisite of a pre-release version and does not have a suffix (e.g., "1.0.0" is stable, "1.0.0-beta" is pre-release).
                        - version 1 means 1.x.x, any major version 1 with any minor and patch version (e.g., "1.0.0", "1.5.6", "1.10.0") is considered version 1.
                        - Handle missing parts by treating them as 0: "2.1" = "2.1.0"
                        - Examples of comparisons:
                          * "2.0.0" > "1.9.9" (20000 > 10909)
                          * "1.10.0" > "1.9.0" (11000 > 10900)
                          * "3.0.0" > "2.99.99" (30000 > 29999)
                          * "1.0.0-beta" < "1.0.0" (pre-release is lower)""",
                """
                        Examples:
                        Input: "1.0.0"
                        Output: return entity.getName().toLowerCase().includes('1.0.0');
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().includes('beta');
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "versions created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "releases created in 2024"
                        Output: return entity.getCreated().getYear() === 2024;
                        
                        Input: "versions created after January 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, entity.getCreated().getOffset()); return entity.getCreated().isAfter(refDate);
                        
                        Input: "releases created before December 2024"
                        Output: const refDate = Java.type('java.time.OffsetDateTime').of(2024, 12, 1, 0, 0, 0, 0, entity.getCreated().getOffset()); return entity.getCreated().isBefore(refDate);
                        
                        Input: "versions updated in 2025"
                        Output: return entity.getUpdated().getYear() === 2025;
                        
                        Input: "versions greater than 2.0.0"
                        Output: const parts = entity.getName().split('.'); if (parts.length < 3) return false; try { const major = parseInt(parts[0]); const minor = parseInt(parts[1]); const patchPart = parts[2]; const patch = parseInt(patchPart.includes('-') ? patchPart.substring(0, patchPart.indexOf('-')) : patchPart); const versionValue = major * 10000 + minor * 100 + patch; return versionValue > 20000; } catch (e) { return false; }
                        
                        Input: "versions less than 1.5.0"
                        Output: const parts = entity.getName().split('.'); if (parts.length < 3) return false; try { const major = parseInt(parts[0]); const minor = parseInt(parts[1]); const patchPart = parts[2]; const patch = parseInt(patchPart.includes('-') ? patchPart.substring(0, patchPart.indexOf('-')) : patchPart); const versionValue = major * 10000 + minor * 100 + patch; return versionValue < 10500; } catch (e) { return false; }
                        
                        Input: "versions between 1.0.0 and 2.0.0"
                        Output: const parts = entity.getName().split('.'); if (parts.length < 3) return false; try { const major = parseInt(parts[0]); const minor = parseInt(parts[1]); const patchPart = parts[2]; const patch = parseInt(patchPart.includes('-') ? patchPart.substring(0, patchPart.indexOf('-')) : patchPart); const versionValue = major * 10000 + minor * 100 + patch; return versionValue >= 10000 && versionValue <= 20000; } catch (e) { return false; }
                        """,
                """
                        Examples:
                        Input: "1.0.0"
                        Output: return entity.getName().toLowerCase().contains("1.0.0");
                        
                        Input: "name contains beta"
                        Output: return entity.getName().toLowerCase().contains("beta");
                        
                        Input: "created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "versions created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "releases created in 2024"
                        Output: return entity.getCreated().getYear() == 2024;
                        
                        Input: "versions created after January 2024"
                        Output: return entity.getCreated().isAfter(OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "releases created before December 2024"
                        Output: return entity.getCreated().isBefore(OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset()));
                        
                        Input: "versions updated in 2025"
                        Output: return entity.getUpdated().getYear() == 2025;
                        
                        Input: "versions greater than 2.0.0"
                        Output: String[] parts = entity.getName().split("\\."); if (parts.length < 3) return false; try { int major = Integer.parseInt(parts[0]); int minor = Integer.parseInt(parts[1]); String patchPart = parts[2]; int patch = Integer.parseInt(patchPart.contains("-") ? patchPart.substring(0, patchPart.indexOf("-")) : patchPart); int versionValue = major * 10000 + minor * 100 + patch; return versionValue > 20000; } catch (NumberFormatException e) { return false; }
                        
                        Input: "versions less than 1.5.0"
                        Output: String[] parts = entity.getName().split("\\."); if (parts.length < 3) return false; try { int major = Integer.parseInt(parts[0]); int minor = Integer.parseInt(parts[1]); String patchPart = parts[2]; int patch = Integer.parseInt(patchPart.contains("-") ? patchPart.substring(0, patchPart.indexOf("-")) : patchPart); int versionValue = major * 10000 + minor * 100 + patch; return versionValue < 10500; } catch (NumberFormatException e) { return false; }
                        
                        Input: "versions between 1.0.0 and 2.0.0"
                        Output: String[] parts = entity.getName().split("\\."); if (parts.length < 3) return false; try { int major = Integer.parseInt(parts[0]); int minor = Integer.parseInt(parts[1]); String patchPart = parts[2]; int patch = Integer.parseInt(patchPart.contains("-") ? patchPart.substring(0, patchPart.indexOf("-")) : patchPart); int versionValue = major * 10000 + minor * 100 + patch; return versionValue >= 10000 && versionValue <= 20000; } catch (NumberFormatException e) { return false; }
                        """
        );
    }
}
