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

package de.bushnaq.abdalla.kassandra.ai.filter;

import de.bushnaq.abdalla.kassandra.ai.filter.dto.version.VersionFilterDto;
import de.bushnaq.abdalla.kassandra.ai.filter.dto.version.VersionFilterPrompt;
import de.bushnaq.abdalla.kassandra.dto.Version;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tests the JavaScript AI filter generator for Version entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same version list. Both the LLM filter and the reference filter operate
 * on {@link VersionFilterDto} objects. The version list is the only place that
 * needs to change when test data is updated; no individual test hard-codes
 * expected indices or counts.</p>
 *
 * <p>For numeric version comparisons the reference JS uses the same
 * {@code major*10000 + minor*100 + patch} weighted scheme documented in
 * {@link VersionFilterPrompt},
 * so both the LLM and the reference are judged by the same rule.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class VersionAiFilterTest extends AbstractAiFilterTest<VersionFilterDto> {

    // -------------------------------------------------------------------------
    // Shared JS helper embedded in every version-comparison reference predicate.
    // Converts "MAJOR.MINOR.PATCH[-suffix]" to an integer weight.
    // Pre-release suffixes do NOT affect the numeric value — the suffix presence
    // is checked separately where needed.
    // -------------------------------------------------------------------------
    private static final String VERSION_VALUE_JS =
            "function versionValue(n) {" +
                    " const p = n.split('.');" +
                    " if (p.length < 3) return -1;" +
                    " const patch = parseInt(p[2].includes('-') ? p[2].substring(0, p[2].indexOf('-')) : p[2]);" +
                    " return parseInt(p[0])*10000 + parseInt(p[1])*100 + patch;" +
                    "} ";

    public VersionAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Version createVersion(UUID id, String name, UUID productId,
                                  OffsetDateTime created, OffsetDateTime updated) {
        Version version = new Version();
        version.setId(id);
        version.setName(name);
        version.setProductId(productId);
        version.setCreated(created);
        version.setUpdated(updated);
        return version;
    }

    @BeforeEach
    void setUp() {
        List<Version> raw = new ArrayList<>();

        // id=1  "1.0.0"          product=1  stable    created 2023-06-15  updated 2024-03-20
        raw.add(createVersion(UUID.randomUUID(), "1.0.0", UUID.randomUUID(),
                OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 20, 14, 30, 0, 0, ZoneOffset.UTC)));

        // id=2  "1.2.3"          product=1  stable    created 2024-01-10  updated 2024-06-05
        raw.add(createVersion(UUID.randomUUID(), "1.2.3", UUID.randomUUID(),
                OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        // id=3  "2.0.0"          product=2  stable    created 2024-02-28  updated 2024-08-12
        raw.add(createVersion(UUID.randomUUID(), "2.0.0", UUID.randomUUID(),
                OffsetDateTime.of(2024, 2, 28, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 12, 13, 20, 0, 0, ZoneOffset.UTC)));

        // id=4  "2.1.5"          product=2  stable    created 2024-04-03  updated 2024-09-18
        raw.add(createVersion(UUID.randomUUID(), "2.1.5", UUID.randomUUID(),
                OffsetDateTime.of(2024, 4, 3, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 18, 12, 10, 0, 0, ZoneOffset.UTC)));

        // id=5  "3.0.0-beta"     product=3  pre-rel   created 2024-07-22  updated 2024-12-01
        raw.add(createVersion(UUID.randomUUID(), "3.0.0-beta", UUID.randomUUID(),
                OffsetDateTime.of(2024, 7, 22, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 12, 1, 10, 25, 0, 0, ZoneOffset.UTC)));

        // id=6  "3.1.0-alpha"    product=3  pre-rel   created 2024-09-05  updated 2025-01-15
        raw.add(createVersion(UUID.randomUUID(), "3.1.0-alpha", UUID.randomUUID(),
                OffsetDateTime.of(2024, 9, 5, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 15, 11, 40, 0, 0, ZoneOffset.UTC)));

        // id=7  "4.0.0-rc1"      product=4  pre-rel   created 2024-11-12  updated 2025-02-08
        raw.add(createVersion(UUID.randomUUID(), "4.0.0-rc1", UUID.randomUUID(),
                OffsetDateTime.of(2024, 11, 12, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 8, 16, 15, 0, 0, ZoneOffset.UTC)));

        // id=8  "0.9.0"          product=4  stable    created 2025-01-05  updated 2025-01-20
        raw.add(createVersion(UUID.randomUUID(), "0.9.0", UUID.randomUUID(),
                OffsetDateTime.of(2025, 1, 5, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 20, 15, 50, 0, 0, ZoneOffset.UTC)));

        // id=9  "1.0.0-SNAPSHOT" product=5  pre-rel   created 2025-02-10  updated 2025-02-25
        raw.add(createVersion(UUID.randomUUID(), "1.0.0-SNAPSHOT", UUID.randomUUID(),
                OffsetDateTime.of(2025, 2, 10, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 25, 17, 30, 0, 0, ZoneOffset.UTC)));

        // id=10 "5.2.1"          product=5  stable    created 2025-03-18  updated 2025-04-02
        raw.add(createVersion(UUID.randomUUID(), "5.2.1", UUID.randomUUID(),
                OffsetDateTime.of(2025, 3, 18, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 2, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (Version v : raw) {
            testProducts.add(VersionFilterDto.from(v));
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1.2.3")
    void test1_2_3() throws Exception {
        assertSearchMatchesReference(
                "1.2.3",
                "Version",
                "return entity.getName().toLowerCase().includes('1.2.3');"
        );
    }

    // --- name / substring filters --------------------------------------------

    @Test
    @DisplayName("alpha")
    void testAlpha() throws Exception {
        assertSearchMatchesReference(
                "alpha",
                "Version",
                "return entity.getName().toLowerCase().includes('alpha');"
        );
    }

    @Test
    @DisplayName("beta")
    void testBeta() throws Exception {
        assertSearchMatchesReference(
                "beta",
                "Version",
                "return entity.getName().toLowerCase().includes('beta');"
        );
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "created in 2025",
                "Version",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "Version",
                "return true;"
        );
    }

    @Test
    @DisplayName("name contains beta")
    void testNameContainsBeta() throws Exception {
        assertSearchMatchesReference(
                "name contains beta",
                "Version",
                "return entity.getName().toLowerCase().includes('beta');"
        );
    }

    @Test
    @DisplayName("pre-release versions")
    void testPreReleaseVersions() throws Exception {
        // A pre-release version is one whose name contains a hyphen
        assertSearchMatchesReference(
                "pre-release versions",
                "Version",
                "return entity.getName().includes('-');"
        );
    }

    // --- major-version prefix filters ----------------------------------------

    @Test
    @DisplayName("rc")
    void testRc() throws Exception {
        assertSearchMatchesReference(
                "rc",
                "Version",
                "return entity.getName().toLowerCase().includes('rc');"
        );
    }

    @Test
    @DisplayName("snapshot")
    void testSnapshot() throws Exception {
        assertSearchMatchesReference(
                "snapshot",
                "Version",
                "return entity.getName().toLowerCase().includes('snapshot');"
        );
    }

    // --- pre-release / stable filters ----------------------------------------

    @Test
    @DisplayName("stable versions (no pre-release suffix)")
    void testStableVersions() throws Exception {
        // A stable version has no hyphen in the name
        assertSearchMatchesReference(
                "stable versions (no pre-release suffix)",
                "Version",
                "return !entity.getName().includes('-');"
        );
    }

    @Test
    @DisplayName("version 1")
    void testVersion1() throws Exception {
        // Names that start with "1." (major version 1)
        assertSearchMatchesReference(
                "version 1",
                "Version",
                "return entity.getName().startsWith('1.');"
        );
    }

    // --- numeric version comparison filters ----------------------------------

    @Test
    @DisplayName("version 3")
    void testVersion3() throws Exception {
        // Names that start with "3." (major version 3)
        assertSearchMatchesReference(
                "version 3",
                "Version",
                "return entity.getName().startsWith('3.');"
        );
    }

    @Test
    @DisplayName("version greater than 2.0.0")
    void testVersionGreaterThan2_0_0() throws Exception {
        // Numeric weight > 20000 (2.0.0 itself is excluded)
        assertSearchMatchesReference(
                "version greater than 2.0.0",
                "Version",
                VERSION_VALUE_JS + "return versionValue(entity.getName()) > 20000;"
        );
    }

    // --- productId filter ----------------------------------------------------

    @Test
    @DisplayName("versions belonging to product 3")
    void testVersionsBelongingToProduct3() throws Exception {
        assertSearchMatchesReference(
                "versions belonging to product 3",
                "Version",
                "return entity.getProductId() === 3;"
        );
    }

    // --- created / updated date filters --------------------------------------

    @Test
    @DisplayName("versions between 1.0.0 and 3.0.0 inclusive")
    void testVersionsBetween1_0_0And3_0_0Inclusive() throws Exception {
        // weight >= 10000 (1.0.0) AND <= 30000 (3.0.0); pre-release suffixes
        // do not change the numeric weight so "3.0.0-beta" (weight=30000) is included
        assertSearchMatchesReference(
                "versions between 1.0.0 and 3.0.0 inclusive",
                "Version",
                VERSION_VALUE_JS + "const v = versionValue(entity.getName()); return v >= 10000 && v <= 30000;"
        );
    }

    @Test
    @DisplayName("versions created after January 31 2024")
    void testVersionsCreatedAfterJanuary31_2024() throws Exception {
        // Explicit day removes ambiguity about "after January 2024"
        assertSearchMatchesReference(
                "versions created after January 31 2024",
                "Version",
                """
                        const boundary = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, Java.type('java.time.ZoneOffset').UTC);
                        return entity.getCreated().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("versions created before March 1 2024")
    void testVersionsCreatedBeforeMarch1_2024() throws Exception {
        // Explicit day removes ambiguity about "before March 2024"
        assertSearchMatchesReference(
                "versions created before March 1 2024",
                "Version",
                """
                        const boundary = Java.type('java.time.OffsetDateTime').of(2024, 3, 1, 0, 0, 0, 0, Java.type('java.time.ZoneOffset').UTC);
                        return entity.getCreated().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("versions updated in 2025")
    void testVersionsUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "versions updated in 2025",
                "Version",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }
}
